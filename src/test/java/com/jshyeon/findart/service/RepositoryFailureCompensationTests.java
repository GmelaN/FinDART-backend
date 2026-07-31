package com.jshyeon.findart.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.doThrow;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import com.jshyeon.findart.MariaDbTestcontainersConfiguration;
import com.jshyeon.findart.document.ContentDocument;
import com.jshyeon.findart.document.ContentDocumentRepository;
import com.jshyeon.findart.document.SummaryDocument;
import com.jshyeon.findart.document.SummaryDocumentRepository;
import com.jshyeon.findart.dto.DailyBriefingIngestion;
import com.jshyeon.findart.dto.IngestionResult;
import com.jshyeon.findart.dto.MarketRegime;
import com.jshyeon.findart.dto.OriginalContentIngestion;
import com.jshyeon.findart.entity.ContentReferenceEntity;
import com.jshyeon.findart.entity.ContentReferenceRepository;
import com.jshyeon.findart.entity.OriginalContentType;
import com.jshyeon.findart.entity.ProcessedContentType;
import com.jshyeon.findart.entity.SummaryReferenceEntity;
import com.jshyeon.findart.entity.SummaryReferenceRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.test.util.AopTestUtils;

@SpringBootTest
@ActiveProfiles("test")
@Import(MariaDbTestcontainersConfiguration.class)
class RepositoryFailureCompensationTests {

	private static final Instant PUBLISHED_AT = Instant.parse("2026-07-20T00:00:00Z");
	private static final Instant COLLECTED_AT = Instant.parse("2026-07-20T00:01:00Z");
	private static final LocalDate BRIEFING_DATE = LocalDate.of(2026, 7, 20);

	@Autowired
	private OriginalContentService originalContentService;

	@Autowired
	private ContentIngestionService contentIngestionService;

	@MockitoSpyBean
	private ContentReferenceRepository contentReferenceRepository;

	@MockitoSpyBean
	private SummaryReferenceRepository summaryReferenceRepository;

	@MockitoSpyBean
	private ContentDocumentRepository contentDocumentRepository;

	@MockitoSpyBean
	private SummaryDocumentRepository summaryDocumentRepository;

	@Autowired
	private JdbcTemplate jdbcTemplate;

	@Autowired
	private MongoTemplate mongoTemplate;

	@BeforeEach
	@AfterEach
	void clearCrossStoreData() {
		jdbcTemplate.update("DELETE FROM summary_content_links");
		jdbcTemplate.update("DELETE FROM summary_references");
		jdbcTemplate.update("DELETE FROM content_references");
		mongoTemplate.remove(new Query(), SummaryDocument.class);
		mongoTemplate.remove(new Query(), ContentDocument.class);
	}

	@Test
	void contentReferenceWriteFailureDeletesAlreadyInsertedMongoDocument() {
		doThrow(databaseFailure()).when(contentReferenceRepository)
			.saveAndFlush(any(ContentReferenceEntity.class));

		assertThrows(DataAccessResourceFailureException.class,
			() -> originalContentService.ingest(original("reference-write-failure", "Body")));

		assertEquals(0, jdbcCount("content_references"));
		assertEquals(0, mongoTemplate.count(new Query(), ContentDocument.class));
	}

	@Test
	void contentReferenceLookupFailureWritesNeitherStore() {
		doThrow(databaseFailure()).when(contentReferenceRepository)
			.findFirstByContentTypeAndSourceAndExternalIdOrderByRevisionDesc(
				any(OriginalContentType.class), any(String.class), any(String.class));

		assertThrows(DataAccessResourceFailureException.class,
			() -> originalContentService.ingest(original("reference-query-failure", "Body")));

		assertEquals(0, jdbcCount("content_references"));
		assertEquals(0, mongoTemplate.count(new Query(), ContentDocument.class));
	}

	@Test
	void mongoInsertFailureWritesNoMariaContentReference() {
		doThrow(databaseFailure()).when(contentDocumentRepository).insert(any(ContentDocument.class));

		assertThrows(DataAccessResourceFailureException.class,
			() -> originalContentService.ingest(original("mongo-insert-failure", "Body")));

		assertEquals(0, jdbcCount("content_references"));
		assertEquals(0, mongoTemplate.count(new Query(), ContentDocument.class));
	}

	@Test
	void originalMongoWriteCannotRunWithoutMariaTransactionSynchronization() {
		OriginalContentService target = AopTestUtils.getUltimateTargetObject(originalContentService);

		IllegalStateException exception = assertThrows(IllegalStateException.class,
			() -> target.ingest(original("missing-content-transaction", "Body")));

		assertTrue(exception.getMessage().contains("MariaDB transaction"));
		assertEquals(0, jdbcCount("content_references"));
		assertEquals(0, mongoTemplate.count(new Query(), ContentDocument.class));
	}

	@Test
	void previousContentMongoUpdateFailureDeletesNewRevisionAndRestoresCurrentFlag() {
		IngestionResult first = originalContentService.ingest(original("content-current-restore", "First body"));
		doThrow(databaseFailure()).when(contentDocumentRepository)
			.save(argThat(document -> Boolean.FALSE.equals(document.getCurrent())));

		assertThrows(DataAccessResourceFailureException.class,
			() -> originalContentService.ingest(original("content-current-restore", "Revised body")));

		ContentDocument restored = contentDocumentRepository.findById(first.getId()).orElseThrow();
		assertTrue(restored.getCurrent());
		assertEquals(1, jdbcCount("content_references"));
		assertEquals(1, mongoTemplate.count(new Query(), ContentDocument.class));
	}

	@Test
	void summaryReferenceWriteFailureDeletesMongoSummaryAndLeavesOriginalUntouched() {
		IngestionResult original = originalContentService.ingest(original("summary-reference-failure", "Body"));
		doThrow(databaseFailure()).when(summaryReferenceRepository)
			.saveAndFlush(any(SummaryReferenceEntity.class));

		assertThrows(DataAccessResourceFailureException.class,
			() -> ingest(briefing("summary-reference-failure", original.getId(), "Summary")));

		assertEquals(1, jdbcCount("content_references"));
		assertEquals(1, mongoTemplate.count(new Query(), ContentDocument.class));
		assertEquals(0, jdbcCount("summary_references"));
		assertEquals(0, jdbcCount("summary_content_links"));
		assertEquals(0, mongoTemplate.count(new Query(), SummaryDocument.class));
	}

	@Test
	void summaryReferenceRevisionFailureRestoresBothPreviousCurrentFlags() {
		IngestionResult original = originalContentService.ingest(original("summary-revision-reference-failure", "Body"));
		IngestionResult first = ingest(briefing("summary-revision-reference-failure", original.getId(), "First"));
		doThrow(databaseFailure()).when(summaryReferenceRepository)
			.saveAndFlush(argThat(reference -> reference.getRevision() == 2));

		assertThrows(DataAccessResourceFailureException.class,
			() -> ingest(briefing("summary-revision-reference-failure", original.getId(), "Revised")));

		SummaryReferenceEntity reference = summaryReferenceRepository.findById(first.getId()).orElseThrow();
		SummaryDocument document = summaryDocumentRepository.findById(first.getId()).orElseThrow();
		assertTrue(reference.isCurrent());
		assertTrue(document.isCurrent());
		assertEquals(1, jdbcCount("summary_references"));
		assertEquals(1, jdbcCount("summary_content_links"));
		assertEquals(1, mongoTemplate.count(new Query(), SummaryDocument.class));
	}

	@Test
	void previousSummaryMongoUpdateFailureDeletesNewRevisionBeforeMariaWrite() {
		IngestionResult original = originalContentService.ingest(original("summary-current-restore", "Body"));
		IngestionResult first = ingest(briefing("summary-current-restore", original.getId(), "First"));
		doThrow(databaseFailure()).when(summaryDocumentRepository)
			.save(argThat(document -> !document.isCurrent()));

		assertThrows(DataAccessResourceFailureException.class,
			() -> ingest(briefing("summary-current-restore", original.getId(), "Revised")));

		SummaryReferenceEntity reference = summaryReferenceRepository.findById(first.getId()).orElseThrow();
		SummaryDocument document = summaryDocumentRepository.findById(first.getId()).orElseThrow();
		assertTrue(reference.isCurrent());
		assertTrue(document.isCurrent());
		assertEquals(1, jdbcCount("summary_references"));
		assertEquals(1, mongoTemplate.count(new Query(), SummaryDocument.class));
	}

	@Test
	void summaryReferenceLookupFailureDoesNotMutateMongoOrMaria() {
		IngestionResult original = originalContentService.ingest(original("summary-query-failure", "Body"));
		doThrow(databaseFailure()).when(summaryReferenceRepository)
			.findFirstBySummaryTypeAndScopeKeyOrderByRevisionDesc(
				any(ProcessedContentType.class), any(String.class));

		assertThrows(DataAccessResourceFailureException.class,
			() -> ingest(briefing("summary-query-failure", original.getId(), "Summary")));

		assertEquals(0, jdbcCount("summary_references"));
		assertEquals(0, mongoTemplate.count(new Query(), SummaryDocument.class));
	}

	@Test
	void mongoSummaryInsertFailureWritesNoMariaSummaryOrProvenance() {
		IngestionResult original = originalContentService.ingest(original("summary-mongo-failure", "Body"));
		doThrow(databaseFailure()).when(summaryDocumentRepository).insert(any(SummaryDocument.class));

		assertThrows(DataAccessResourceFailureException.class,
			() -> ingest(briefing("summary-mongo-failure", original.getId(), "Summary")));

		assertEquals(0, jdbcCount("summary_references"));
		assertEquals(0, jdbcCount("summary_content_links"));
		assertEquals(0, mongoTemplate.count(new Query(), SummaryDocument.class));
		assertFalse(contentDocumentRepository.findAll().isEmpty());
	}

	@Test
	void summaryMongoWriteCannotRunWithoutMariaTransactionSynchronization() {
		IngestionResult original = originalContentService.ingest(original("missing-summary-transaction", "Body"));
		ContentIngestionService target = AopTestUtils.getUltimateTargetObject(contentIngestionService);

		IllegalStateException exception = assertThrows(IllegalStateException.class,
			() -> {
				DailyBriefingIngestion request = briefing(
					"missing-summary-transaction", original.getId(), "Summary");
				target.ingest(ProcessedContentType.DAILY_BRIEFING, request.getSource(), request.getExternalId(),
					request.getCollectedAt(), request.getBriefingDate(), request.getPublishedAt(),
					request.getOriginalContentIds(), request);
			});

		assertTrue(exception.getMessage().contains("MariaDB transaction"));
		assertEquals(0, jdbcCount("summary_references"));
		assertEquals(0, mongoTemplate.count(new Query(), SummaryDocument.class));
	}

	private int jdbcCount(String table) {
		return jdbcTemplate.queryForObject("SELECT COUNT(*) FROM " + table, Integer.class);
	}

	private IngestionResult ingest(DailyBriefingIngestion request) {
		return contentIngestionService.ingest(ProcessedContentType.DAILY_BRIEFING, request.getSource(),
			request.getExternalId(), request.getCollectedAt(), request.getBriefingDate(), request.getPublishedAt(),
			request.getOriginalContentIds(), request);
	}

	private OriginalContentIngestion original(String externalId, String body) {
		return new OriginalContentIngestion(OriginalContentType.ARTICLE, "test-source", externalId,
			"https://example.com/" + externalId, "Title", body, "Example", "ko",
			Map.of("purpose", "failure-test"), PUBLISHED_AT, COLLECTED_AT);
	}

	private DailyBriefingIngestion briefing(String externalId, String originalId, String summary) {
		return new DailyBriefingIngestion("test-collector", externalId, COLLECTED_AT, null, List.of(originalId),
			BRIEFING_DATE, DailyBriefingIngestion.Mode.DAILY, "Today", summary,
			List.of(new MarketRegime(MarketRegime.Category.INTEREST_RATE, "STABLE", "Test rationale")),
			List.of(), List.of(), List.of(), List.of(), PUBLISHED_AT);
	}

	private DataAccessResourceFailureException databaseFailure() {
		return new DataAccessResourceFailureException("Injected repository failure");
	}
}
