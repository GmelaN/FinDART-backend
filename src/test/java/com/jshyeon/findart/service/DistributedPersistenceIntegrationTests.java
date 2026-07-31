package com.jshyeon.findart.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import com.jshyeon.findart.MariaDbTestcontainersConfiguration;
import com.jshyeon.findart.document.ContentDocument;
import com.jshyeon.findart.document.ContentDocumentRepository;
import com.jshyeon.findart.document.SummaryDocument;
import com.jshyeon.findart.document.SummaryDocumentRepository;
import com.jshyeon.findart.dto.DailyBriefingIngestion;
import com.jshyeon.findart.dto.EconomyOverviewIngestion;
import com.jshyeon.findart.dto.FeaturedCompany;
import com.jshyeon.findart.dto.FeaturedIndustryIngestion;
import com.jshyeon.findart.dto.IngestionResult;
import com.jshyeon.findart.dto.IndicatorCard;
import com.jshyeon.findart.dto.MarketRegime;
import com.jshyeon.findart.dto.OriginalContentIngestion;
import com.jshyeon.findart.dto.PolicyBriefingIngestion;
import com.jshyeon.findart.dto.PolicyEvidence;
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
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

@SpringBootTest
@ActiveProfiles("test")
@Import(MariaDbTestcontainersConfiguration.class)
class DistributedPersistenceIntegrationTests {

	private static final Instant PUBLISHED_AT = Instant.parse("2026-07-20T00:00:00Z");
	private static final Instant COLLECTED_AT = Instant.parse("2026-07-20T00:01:00Z");
	private static final LocalDate BRIEFING_DATE = LocalDate.of(2026, 7, 20);

	@Autowired
	private OriginalContentService originalContentService;

	@Autowired
	private ContentIngestionService contentIngestionService;

	@Autowired
	private ContentQueryService contentQueryService;

	@Autowired
	private ContentReferenceRepository contentReferenceRepository;

	@Autowired
	private SummaryReferenceRepository summaryReferenceRepository;

	@Autowired
	private ContentDocumentRepository contentDocumentRepository;

	@Autowired
	private SummaryDocumentRepository summaryDocumentRepository;

	@Autowired
	private JdbcTemplate jdbcTemplate;

	@Autowired
	private PlatformTransactionManager transactionManager;

	@BeforeEach
	@AfterEach
	void clearCrossStoreData() {
		jdbcTemplate.update("DELETE FROM summary_content_links");
		jdbcTemplate.update("DELETE FROM summary_references");
		jdbcTemplate.update("DELETE FROM content_references");
		summaryDocumentRepository.deleteAll();
		contentDocumentRepository.deleteAll();
	}

	@Test
	void originalContentIsSplitAcrossBothStoresAndReassembled() {
		IngestionResult result = originalContentService.ingest(original("article-1", "Original body"));

		ContentReferenceEntity reference = contentReferenceRepository.findById(result.getId()).orElseThrow();
		ContentDocument document = contentDocumentRepository.findById(result.getId()).orElseThrow();
		var response = originalContentService.get(result.getId());

		assertEquals(IngestionResult.Status.CREATED, result.getStatus());
		assertEquals("findart", reference.getMongoDatabase());
		assertEquals("contents", reference.getMongoCollection());
		assertEquals("article-1", reference.getExternalId());
		assertEquals(reference.getId(), document.getId());
		assertEquals("Original body", document.getBody().getRaw());
		assertEquals("schema-test", response.getAttributes().get("purpose").asString());
		assertEquals("Original body", response.getRawBody());
	}

	@Test
	void duplicateOriginalContentDoesNotCreateAnotherReferenceOrDocument() {
		OriginalContentIngestion request = original("article-duplicate", "Same body");

		IngestionResult first = originalContentService.ingest(request);
		IngestionResult duplicate = originalContentService.ingest(request);

		assertEquals(IngestionResult.Status.CREATED, first.getStatus());
		assertEquals(IngestionResult.Status.DUPLICATE, duplicate.getStatus());
		assertEquals(first.getId(), duplicate.getId());
		assertEquals(1, contentReferenceRepository.count());
		assertEquals(1, contentDocumentRepository.count());
	}

	@Test
	void revisedOriginalContentKeepsHistoryAndSwitchesMongoCurrentRevision() {
		IngestionResult first = originalContentService.ingest(original("article-revision", "First body"));
		IngestionResult revised = originalContentService.ingest(original("article-revision", "Revised body"));

		ContentDocument firstDocument = contentDocumentRepository.findById(first.getId()).orElseThrow();
		ContentDocument revisedDocument = contentDocumentRepository.findById(revised.getId()).orElseThrow();

		assertEquals(IngestionResult.Status.REVISED, revised.getStatus());
		assertEquals(2, revised.getRevision());
		assertNotEquals(first.getId(), revised.getId());
		assertFalse(firstDocument.getCurrent());
		assertTrue(revisedDocument.getCurrent());
		assertEquals(2, contentReferenceRepository.count());
		assertEquals(2, contentDocumentRepository.count());
	}

	@Test
	void realMariaConstraintFailureRemovesNewMongoContent() {
		OriginalContentIngestion request = original("article-too-long-source", "Body");
		request.setSource("x".repeat(101));

		assertThrows(RuntimeException.class, () -> originalContentService.ingest(request));

		assertEquals(0, contentReferenceRepository.count());
		assertEquals(0, contentDocumentRepository.count());
	}

	@Test
	void outerMariaTransactionRollbackAlsoRemovesMongoContent() {
		TransactionTemplate transaction = new TransactionTemplate(transactionManager);

		transaction.executeWithoutResult(status -> {
			originalContentService.ingest(original("article-outer-rollback", "Body"));
			status.setRollbackOnly();
		});

		assertEquals(0, contentReferenceRepository.count());
		assertEquals(0, contentDocumentRepository.count());
	}

	@Test
	void originalReadDetectsDanglingMariaReference() {
		IngestionResult result = originalContentService.ingest(original("article-missing-document", "Body"));
		contentDocumentRepository.deleteById(result.getId());

		IllegalStateException exception = assertThrows(
			IllegalStateException.class, () -> originalContentService.get(result.getId()));

		assertTrue(exception.getMessage().contains(result.getId()));
	}

	@Test
	void summaryStoresPayloadAndRelationalProvenanceAndCanBeQueried() {
		IngestionResult original = originalContentService.ingest(original("article-for-summary", "Source body"));
		DailyBriefingIngestion request = briefing("briefing-1", original.getId(), "Summary text");

		IngestionResult summary = ingest(request);
		SummaryReferenceEntity reference = summaryReferenceRepository.findById(summary.getId()).orElseThrow();
		SummaryDocument document = summaryDocumentRepository.findById(summary.getId()).orElseThrow();
		var response = contentQueryService.processedContent(summary.getId());

		assertEquals("findart", reference.getMongoDatabase());
		assertEquals("summaries", reference.getMongoCollection());
		assertEquals(original.getId(), jdbcTemplate.queryForObject(
			"SELECT content_id FROM summary_content_links WHERE summary_id = ?",
			String.class, summary.getId()));
		assertEquals(List.of(original.getId()), document.getSources().getContentIds());
		assertEquals("Summary text", document.getSummaryText());
		assertEquals(original.getId(), response.getOriginalContentIds().iterator().next());
		assertEquals("Summary text", response.getContent().get("summary").asString());
		assertEquals(1, jdbcTemplate.queryForObject("SELECT COUNT(*) FROM summary_content_links", Integer.class));
	}

	@Test
	void duplicateSummaryDoesNotCreateAnotherReferenceDocumentOrLink() {
		IngestionResult original = originalContentService.ingest(original("article-summary-duplicate", "Body"));
		DailyBriefingIngestion request = briefing("briefing-duplicate", original.getId(), "Same summary");

		IngestionResult first = ingest(request);
		IngestionResult duplicate = ingest(request);

		assertEquals(IngestionResult.Status.DUPLICATE, duplicate.getStatus());
		assertEquals(first.getId(), duplicate.getId());
		assertEquals(1, summaryReferenceRepository.count());
		assertEquals(1, summaryDocumentRepository.count());
		assertEquals(1, jdbcTemplate.queryForObject("SELECT COUNT(*) FROM summary_content_links", Integer.class));
	}

	@Test
	void revisedSummarySwitchesCurrentRevisionAndQueriesOnlyTheLatestPayload() {
		IngestionResult original = originalContentService.ingest(original("article-summary-revision", "Body"));
		IngestionResult first = ingest(briefing("briefing-revision", original.getId(), "First summary"));
		IngestionResult revised = ingest(briefing("briefing-revision", original.getId(), "Revised summary"));

		SummaryReferenceEntity firstReference = summaryReferenceRepository.findById(first.getId()).orElseThrow();
		SummaryReferenceEntity revisedReference = summaryReferenceRepository.findById(revised.getId()).orElseThrow();
		SummaryDocument firstDocument = summaryDocumentRepository.findById(first.getId()).orElseThrow();
		SummaryDocument revisedDocument = summaryDocumentRepository.findById(revised.getId()).orElseThrow();
		var current = contentQueryService.processedContents(ProcessedContentType.DAILY_BRIEFING);

		assertFalse(firstReference.isCurrent());
		assertTrue(revisedReference.isCurrent());
		assertFalse(firstDocument.isCurrent());
		assertTrue(revisedDocument.isCurrent());
		assertEquals(2, revised.getRevision());
		assertEquals(1, current.size());
		assertEquals(revised.getId(), current.getFirst().getId());
		assertEquals("Revised summary", current.getFirst().getContent().get("summary").asString());
	}

	@Test
	void realSummaryConstraintFailureRemovesMongoSummaryAndRelationalLinks() {
		IngestionResult original = originalContentService.ingest(original("article-summary-failure", "Body"));
		DailyBriefingIngestion request = briefing("briefing-null-cutoff", original.getId(), "Summary");

		assertThrows(RuntimeException.class, () -> contentIngestionService.ingest(
			ProcessedContentType.DAILY_BRIEFING, request.getSource(), request.getExternalId(), null,
			request.getBriefingDate(), request.getPublishedAt(), request.getOriginalContentIds(), request));

		assertEquals(0, summaryReferenceRepository.count());
		assertEquals(0, summaryDocumentRepository.count());
		assertEquals(0, jdbcTemplate.queryForObject("SELECT COUNT(*) FROM summary_content_links", Integer.class));
		assertEquals(1, contentReferenceRepository.count());
		assertEquals(1, contentDocumentRepository.count());
	}

	@Test
	void unknownOriginalReferenceIsRejectedBeforeEitherSummaryStoreIsWritten() {
		DailyBriefingIngestion request = briefing(
			"briefing-unknown-original", UUID.randomUUID().toString(), "Summary");

		assertThrows(IllegalArgumentException.class, () -> ingest(request));

		assertEquals(0, summaryReferenceRepository.count());
		assertEquals(0, summaryDocumentRepository.count());
	}

	@Test
	void outerRollbackOfOriginalAndSummaryRestoresBothStoresToEmpty() {
		TransactionTemplate transaction = new TransactionTemplate(transactionManager);

		transaction.executeWithoutResult(status -> {
			IngestionResult original = originalContentService.ingest(original("article-full-rollback", "Body"));
			ingest(briefing("briefing-full-rollback", original.getId(), "Summary"));
			status.setRollbackOnly();
		});

		assertEquals(0, contentReferenceRepository.count());
		assertEquals(0, summaryReferenceRepository.count());
		assertEquals(0, contentDocumentRepository.count());
		assertEquals(0, summaryDocumentRepository.count());
		assertEquals(0, jdbcTemplate.queryForObject("SELECT COUNT(*) FROM summary_content_links", Integer.class));
	}

	@Test
	void summaryReadDetectsDanglingMariaReference() {
		IngestionResult original = originalContentService.ingest(original("article-missing-summary", "Body"));
		IngestionResult summary = ingest(briefing("briefing-missing-document", original.getId(), "Summary"));
		summaryDocumentRepository.deleteById(summary.getId());

		IllegalStateException exception = assertThrows(
			IllegalStateException.class, () -> contentQueryService.processedContent(summary.getId()));

		assertTrue(exception.getMessage().contains(summary.getId()));
	}

	@Test
	void todayQueryHydratesMongoPayloadSelectedByMariaCurrentReference() {
		IngestionResult original = originalContentService.ingest(original("article-today", "Body"));
		IngestionResult first = ingest(briefing("briefing-today", original.getId(), "Old market summary"));
		IngestionResult revised = ingest(briefing("briefing-today", original.getId(), "Current market summary"));

		var response = contentQueryService.today(BRIEFING_DATE);

		assertEquals(revised.getId(), response.getId());
		assertNotEquals(first.getId(), response.getId());
		assertEquals("Current market summary", response.getSummary());
	}

	@Test
	void economyQuerySelectsLatestMongoPayloadNotAfterRequestedDate() {
		IngestionResult original = originalContentService.ingest(original("article-economy", "Body"));
		EconomyOverviewIngestion older = economyOverview(
			"economy-older", original.getId(), LocalDate.of(2026, 7, 19), "Older overview");
		EconomyOverviewIngestion future = economyOverview(
			"economy-future", original.getId(), LocalDate.of(2026, 7, 21), "Future overview");
		ingest(ProcessedContentType.ECONOMY_OVERVIEW, older.getExternalId(), older.getAsOfDate(), older);
		ingest(ProcessedContentType.ECONOMY_OVERVIEW, future.getExternalId(), future.getAsOfDate(), future);

		var response = contentQueryService.economyOverview(LocalDate.of(2026, 7, 20));

		assertEquals(LocalDate.of(2026, 7, 19), response.getAsOfDate());
		assertEquals("Older overview", response.getAbstractText());
		assertEquals(1, response.getIndicatorCards().size());
		assertEquals(IndicatorCard.Indicator.INTEREST_RATE,
			response.getIndicatorCards().getFirst().getIndicator());
	}

	@Test
	void policyQueryHydratesMongoBodiesAndSortsByPublishedTime() {
		IngestionResult original = originalContentService.ingest(original("article-policy", "Body"));
		PolicyBriefingIngestion older = policyBriefing(
			"policy-older", original.getId(), "Older policy", Instant.parse("2026-07-19T00:00:00Z"));
		PolicyBriefingIngestion newer = policyBriefing(
			"policy-newer", original.getId(), "Newer policy", Instant.parse("2026-07-21T00:00:00Z"));
		ingest(ProcessedContentType.POLICY_BRIEFING, older.getExternalId(),
			older.getPublishedAt().atZone(java.time.ZoneOffset.UTC).toLocalDate(), older);
		IngestionResult newerResult = ingest(ProcessedContentType.POLICY_BRIEFING, newer.getExternalId(),
			newer.getPublishedAt().atZone(java.time.ZoneOffset.UTC).toLocalDate(), newer);

		var responses = contentQueryService.policyBriefings();
		var detail = contentQueryService.policyBriefing(newerResult.getId());

		assertEquals(List.of("Newer policy", "Older policy"),
			responses.stream().map(response -> response.getTitle()).toList());
		assertEquals("Newer policy body", detail.getBody());
		assertEquals("Policy evidence", detail.getEvidence().getFirst().getTitle());
	}

	@Test
	void featuredIndustryQueryUsesMongoValidityWindowAndMariaCurrentReferences() {
		IngestionResult original = originalContentService.ingest(original("article-industry", "Body"));
		FeaturedIndustryIngestion active = featuredIndustry(
			"industry-active", original.getId(), "Active industry",
			LocalDate.of(2026, 7, 1), LocalDate.of(2026, 7, 31));
		FeaturedIndustryIngestion expired = featuredIndustry(
			"industry-expired", original.getId(), "Expired industry",
			LocalDate.of(2026, 6, 1), LocalDate.of(2026, 6, 30));
		IngestionResult activeResult = ingest(
			ProcessedContentType.FEATURED_INDUSTRY, active.getExternalId(), active.getValidFrom(), active);
		ingest(ProcessedContentType.FEATURED_INDUSTRY, expired.getExternalId(), expired.getValidFrom(), expired);

		var responses = contentQueryService.featuredIndustries(LocalDate.of(2026, 7, 20));
		var detail = contentQueryService.featuredIndustry(activeResult.getId());

		assertEquals(1, responses.size());
		assertEquals(activeResult.getId(), responses.getFirst().getId());
		assertEquals("Active industry", detail.getTitle());
		assertEquals("Example Corp", detail.getCompanies().getFirst().getName());
	}

	@Test
	void weeklySummaryPersistsWeekGrainAndUtcPeriodBounds() {
		IngestionResult original = originalContentService.ingest(original("article-weekly", "Body"));
		DailyBriefingIngestion request = briefing("briefing-weekly", original.getId(), "Weekly summary");
		request.setBriefingDate(LocalDate.of(2026, 7, 25));
		request.setMode(DailyBriefingIngestion.Mode.WEEKLY_RECAP);

		IngestionResult result = ingest(request);
		SummaryReferenceEntity reference = summaryReferenceRepository.findById(result.getId()).orElseThrow();
		SummaryDocument document = summaryDocumentRepository.findById(result.getId()).orElseThrow();

		assertEquals("WEEK", reference.getTimeGrain());
		assertEquals(Instant.parse("2026-07-19T15:00:00Z"), reference.getPeriodStart());
		assertEquals(Instant.parse("2026-07-25T15:00:00Z"), reference.getPeriodEnd());
		assertEquals(reference.getPeriodStart(), document.getPeriod().getStart());
		assertEquals(reference.getPeriodEnd(), document.getPeriod().getEnd());
	}

	private IngestionResult ingest(DailyBriefingIngestion request) {
		return contentIngestionService.ingest(ProcessedContentType.DAILY_BRIEFING, request.getSource(),
			request.getExternalId(), request.getCollectedAt(), request.getBriefingDate(), request.getPublishedAt(),
			request.getOriginalContentIds(), request);
	}

	private IngestionResult ingest(ProcessedContentType type, String externalId, LocalDate effectiveDate,
			Object content) {
		List<String> originalIds;
		if (content instanceof EconomyOverviewIngestion value) {
			originalIds = value.getOriginalContentIds();
		} else if (content instanceof PolicyBriefingIngestion value) {
			originalIds = value.getOriginalContentIds();
		} else if (content instanceof FeaturedIndustryIngestion value) {
			originalIds = value.getOriginalContentIds();
		} else {
			throw new IllegalArgumentException("Unsupported test payload.");
		}
		return contentIngestionService.ingest(type, "test-collector", externalId, COLLECTED_AT,
			effectiveDate, PUBLISHED_AT, originalIds, content);
	}

	private OriginalContentIngestion original(String externalId, String body) {
		return new OriginalContentIngestion(OriginalContentType.ARTICLE, "test-source", externalId,
			"https://example.com/" + externalId, "Title " + externalId, body, "Example", "ko",
			Map.of("purpose", "schema-test"), PUBLISHED_AT, COLLECTED_AT);
	}

	private DailyBriefingIngestion briefing(String externalId, String originalId, String summary) {
		return new DailyBriefingIngestion("test-collector", externalId, COLLECTED_AT, null, List.of(originalId),
			BRIEFING_DATE, DailyBriefingIngestion.Mode.DAILY, "Today", summary,
			List.of(new MarketRegime(MarketRegime.Category.INTEREST_RATE, "STABLE", "Test rationale")),
			List.of(), List.of(), List.of(), List.of(), PUBLISHED_AT);
	}

	private EconomyOverviewIngestion economyOverview(
			String externalId, String originalId, LocalDate date, String abstractText) {
		return new EconomyOverviewIngestion("test-collector", externalId, COLLECTED_AT, null, List.of(originalId),
			date, List.of(new IndicatorCard(IndicatorCard.Indicator.INTEREST_RATE, 2.5, "%",
				date.toString(), 0.1, IndicatorCard.Direction.FLAT, IndicatorCard.Horizon.QUARTERLY,
				"Stable rates", List.of())), List.of(), abstractText, PUBLISHED_AT);
	}

	private PolicyBriefingIngestion policyBriefing(
			String externalId, String originalId, String title, Instant publishedAt) {
		return new PolicyBriefingIngestion("test-collector", externalId, COLLECTED_AT, null, List.of(originalId),
			title, title + " body", publishedAt, List.of(new PolicyEvidence(
				PolicyEvidence.DocumentType.POLICY_BRIEFING, "Policy evidence", "Ministry", publishedAt,
				"https://example.com/policy")));
	}

	private FeaturedIndustryIngestion featuredIndustry(String externalId, String originalId, String title,
			LocalDate validFrom, LocalDate validTo) {
		return new FeaturedIndustryIngestion("test-collector", externalId, COLLECTED_AT, null, List.of(originalId),
			"Technology", "Semiconductor", title, title + " rationale", "Positive", "Negative",
			validFrom, validTo, List.of(), List.of(new FeaturedCompany(
				"Example Corp", "000001", "Semiconductors", FeaturedCompany.CompanySize.LARGE_CAP,
				FeaturedCompany.OperatingProfitTrend.IMPROVING)));
	}
}
