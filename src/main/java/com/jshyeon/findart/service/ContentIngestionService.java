package com.jshyeon.findart.service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import com.jshyeon.findart.document.SummaryDocument;
import com.jshyeon.findart.document.SummaryDocumentRepository;
import com.jshyeon.findart.dto.DailyBriefingIngestion;
import com.jshyeon.findart.dto.EconomyOverviewIngestion;
import com.jshyeon.findart.dto.FeaturedIndustryIngestion;
import com.jshyeon.findart.dto.IngestionResult;
import com.jshyeon.findart.dto.PolicyBriefingIngestion;
import com.jshyeon.findart.entity.ProcessedContentType;
import com.jshyeon.findart.entity.SummaryReferenceEntity;
import com.jshyeon.findart.entity.SummaryReferenceRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;

@Service
@RequiredArgsConstructor
@Slf4j
public class ContentIngestionService {

	private static final ZoneId KOREA = ZoneId.of("Asia/Seoul");

	private final SummaryReferenceRepository referenceRepository;
	private final SummaryDocumentRepository documentRepository;
	private final OriginalContentService originalContentService;
	private final ObjectMapper objectMapper;

	@Transactional
	public IngestionResult ingest(ProcessedContentType type, String source, String externalId, Instant collectedAt,
			LocalDate effectiveDate, Instant publishedAt, List<String> originalContentIds, Object content) {
		if (originalContentIds.stream().anyMatch(id -> !originalContentService.exists(id))) {
			throw new IllegalArgumentException("Processed content references an unknown original content ID.");
		}

		String serializedPayload = serialize(content);
		String checksum = sha256(serializedPayload);
		String scopeKey = "GLOBAL:" + sha256(source + '\u0000' + externalId);
		var latestReference = referenceRepository.findFirstBySummaryTypeAndScopeKeyOrderByRevisionDesc(type, scopeKey);
		SummaryDocument previous = latestReference.map(this::requiredDocument).orElse(null);
		if (previous != null && sha256(serialize(previous.getPayload())).equals(checksum)) {
			return new IngestionResult(previous.getId(), previous.getRevision(), IngestionResult.Status.DUPLICATE);
		}

		int revision = latestReference.map(value -> value.getRevision() + 1).orElse(1);
		Map<String, Object> payload = payload(serializedPayload);
		PeriodBounds bounds = periodBounds(content, effectiveDate);
		String timeGrain = timeGrain(content);
		String id = UUID.randomUUID().toString();
		SummaryDocument document = new SummaryDocument(id, type, scopeKey, timeGrain, bounds.start(), bounds.end(),
			title(content, type, effectiveDate), summaryText(content), payload, originalContentIds, revision);

		registerMongoRollbackCompensation(id, previous);
		documentRepository.insert(document);
		if (previous != null) {
			previous.markNotCurrent();
			documentRepository.save(previous);
			latestReference.orElseThrow().markNotCurrent();
		}

		referenceRepository.saveAndFlush(new SummaryReferenceEntity(id, type, scopeKey, timeGrain, bounds.start(),
			bounds.end(), collectedAt, revision, new LinkedHashSet<>(originalContentIds)));
		return new IngestionResult(id, revision,
			revision == 1 ? IngestionResult.Status.CREATED : IngestionResult.Status.REVISED);
	}

	private SummaryDocument requiredDocument(SummaryReferenceEntity reference) {
		return documentRepository.findById(reference.getId())
			.orElseThrow(() -> new IllegalStateException("MongoDB summary " + reference.getId() + " is missing."));
	}

	private void registerMongoRollbackCompensation(String newDocumentId, SummaryDocument previous) {
		if (!TransactionSynchronizationManager.isSynchronizationActive()) {
			throw new IllegalStateException("A MariaDB transaction is required before writing MongoDB summaries.");
		}
		TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
			@Override
			public void afterCompletion(int status) {
				if (status != STATUS_COMMITTED) {
					try {
						documentRepository.deleteById(newDocumentId);
					} catch (RuntimeException exception) {
						log.error("Could not remove rolled-back MongoDB summary {}.", newDocumentId, exception);
					}
					if (previous != null) {
						try {
							previous.markCurrent();
							documentRepository.save(previous);
						} catch (RuntimeException exception) {
							log.error("Could not restore MongoDB summary revision {}.", previous.getId(), exception);
						}
					}
				}
			}
		});
	}

	@SuppressWarnings("unchecked")
	private Map<String, Object> payload(String serializedPayload) {
		try {
			return objectMapper.readValue(serializedPayload, LinkedHashMap.class);
		} catch (JacksonException exception) {
			throw new IllegalStateException("Could not create the MongoDB summary payload.", exception);
		}
	}

	private String title(Object content, ProcessedContentType type, LocalDate effectiveDate) {
		if (content instanceof DailyBriefingIngestion value) {
			return value.getTitle();
		}
		if (content instanceof PolicyBriefingIngestion value) {
			return value.getTitle();
		}
		if (content instanceof FeaturedIndustryIngestion value) {
			return value.getTitle();
		}
		return type.name() + " " + effectiveDate;
	}

	private String summaryText(Object content) {
		if (content instanceof DailyBriefingIngestion value) {
			return value.getSummary();
		}
		if (content instanceof EconomyOverviewIngestion value) {
			return value.getAbstractText();
		}
		if (content instanceof PolicyBriefingIngestion value) {
			return value.getBody();
		}
		if (content instanceof FeaturedIndustryIngestion value) {
			return value.getRationale();
		}
		throw new IllegalArgumentException("Unsupported summary content type: " + content.getClass().getName());
	}

	private String timeGrain(Object content) {
		if (content instanceof DailyBriefingIngestion value && value.getMode() != DailyBriefingIngestion.Mode.DAILY) {
			return "WEEK";
		}
		return "DAY";
	}

	private PeriodBounds periodBounds(Object content, LocalDate effectiveDate) {
		LocalDate start = effectiveDate;
		LocalDate end = effectiveDate.plusDays(1);
		if (content instanceof DailyBriefingIngestion value) {
			if (value.getMode() == DailyBriefingIngestion.Mode.WEEKLY_RECAP) {
				start = effectiveDate.minusDays(5);
			} else if (value.getMode() == DailyBriefingIngestion.Mode.NEXT_WEEK_OUTLOOK) {
				start = effectiveDate.plusDays(1);
				end = start.plusWeeks(1);
			}
		} else if (content instanceof FeaturedIndustryIngestion value && value.getValidTo() != null) {
			end = value.getValidTo().plusDays(1);
		}
		return new PeriodBounds(start.atStartOfDay(KOREA).toInstant(), end.atStartOfDay(KOREA).toInstant());
	}

	private String serialize(Object content) {
		try {
			return objectMapper.writeValueAsString(content);
		} catch (JacksonException exception) {
			throw new IllegalStateException("Could not serialize collected content.", exception);
		}
	}

	private String sha256(String payload) {
		try {
			byte[] digest = MessageDigest.getInstance("SHA-256").digest(payload.getBytes(StandardCharsets.UTF_8));
			return java.util.HexFormat.of().formatHex(digest);
		} catch (NoSuchAlgorithmException exception) {
			throw new IllegalStateException("SHA-256 is unavailable.", exception);
		}
	}

	private record PeriodBounds(Instant start, Instant end) {
	}
}
