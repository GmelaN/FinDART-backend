package com.jshyeon.findart.service;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import com.jshyeon.findart.document.SummaryDocument;
import com.jshyeon.findart.document.SummaryDocumentRepository;
import com.jshyeon.findart.dto.DailyBriefingIngestion;
import com.jshyeon.findart.dto.EconomyOverviewIngestion;
import com.jshyeon.findart.dto.EconomyOverviewResponse;
import com.jshyeon.findart.dto.FeaturedIndustryIngestion;
import com.jshyeon.findart.dto.FeaturedIndustryResponse;
import com.jshyeon.findart.dto.PolicyBriefingIngestion;
import com.jshyeon.findart.dto.PolicyBriefingResponse;
import com.jshyeon.findart.dto.ProcessedContentResponse;
import com.jshyeon.findart.dto.TodayBriefingResponse;
import com.jshyeon.findart.entity.ProcessedContentType;
import com.jshyeon.findart.entity.SummaryReferenceEntity;
import com.jshyeon.findart.entity.SummaryReferenceRepository;
import com.jshyeon.findart.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class ContentQueryService {

	private static final ZoneId KOREA = ZoneId.of("Asia/Seoul");

	private final SummaryReferenceRepository referenceRepository;
	private final SummaryDocumentRepository documentRepository;
	private final ObjectMapper objectMapper;

	public TodayBriefingResponse today(LocalDate date) {
		DailyBriefingIngestion.Mode mode = switch (date.getDayOfWeek()) {
			case SATURDAY -> DailyBriefingIngestion.Mode.WEEKLY_RECAP;
			case SUNDAY -> DailyBriefingIngestion.Mode.NEXT_WEEK_OUTLOOK;
			default -> DailyBriefingIngestion.Mode.DAILY;
		};
		return latest(ProcessedContentType.DAILY_BRIEFING).stream()
			.map(document -> Map.entry(document, read(document, DailyBriefingIngestion.class)))
			.filter(entry -> entry.getValue().getBriefingDate().equals(date) && entry.getValue().getMode() == mode)
			.findFirst()
			.map(entry -> {
				DailyBriefingIngestion content = entry.getValue();
				return new TodayBriefingResponse(entry.getKey().getId(), content.getBriefingDate(), content.getMode(),
					content.getTitle(), content.getSummary(), content.getMarket(), content.getHeadlines(),
					content.getIssues(), content.getIssueTracking(), content.getEvents(), content.getPublishedAt());
			})
			.orElseThrow(() -> new ResourceNotFoundException("No briefing is available for " + date + "."));
	}

	public EconomyOverviewResponse economyOverview(LocalDate asOfDate) {
		var entry = latest(ProcessedContentType.ECONOMY_OVERVIEW).stream()
			.map(document -> Map.entry(document, read(document, EconomyOverviewIngestion.class)))
			.filter(candidate -> !candidate.getValue().getAsOfDate().isAfter(asOfDate))
			.max(Comparator.comparing(candidate -> candidate.getValue().getAsOfDate()))
			.orElseThrow(() -> new ResourceNotFoundException(
				"No economy overview is available on or before " + asOfDate + "."));
		EconomyOverviewIngestion content = entry.getValue();
		return new EconomyOverviewResponse(entry.getKey().getId(), content.getAsOfDate(), content.getIndicatorCards(),
			content.getScheduledEvents(), content.getAbstractText(), content.getPublishedAt());
	}

	public List<PolicyBriefingResponse> policyBriefings() {
		return latest(ProcessedContentType.POLICY_BRIEFING).stream()
			.map(document -> {
				PolicyBriefingIngestion content = read(document, PolicyBriefingIngestion.class);
				return new PolicyBriefingResponse(document.getId(), content.getTitle(), content.getBody(),
					content.getPublishedAt(), content.getEvidence());
			})
			.sorted(Comparator.comparing(PolicyBriefingResponse::getPublishedAt).reversed())
			.toList();
	}

	public PolicyBriefingResponse policyBriefing(String id) {
		return policyBriefings().stream().filter(briefing -> briefing.getId().equals(id)).findFirst()
			.orElseThrow(() -> new ResourceNotFoundException("Policy briefing " + id + " was not found."));
	}

	public List<FeaturedIndustryResponse> featuredIndustries(LocalDate asOfDate) {
		return latest(ProcessedContentType.FEATURED_INDUSTRY).stream()
			.map(document -> {
				FeaturedIndustryIngestion content = read(document, FeaturedIndustryIngestion.class);
				return new FeaturedIndustryResponse(document.getId(), content.getSector(), content.getSegment(),
					content.getTitle(), content.getRationale(), content.getPositiveScenario(),
					content.getNegativeScenario(), content.getValidFrom(), content.getValidTo(),
					content.getEvidence(), content.getCompanies());
			})
			.filter(industry -> !industry.getValidFrom().isAfter(asOfDate)
				&& (industry.getValidTo() == null || !industry.getValidTo().isBefore(asOfDate)))
			.toList();
	}

	public FeaturedIndustryResponse featuredIndustry(String id) {
		return latest(ProcessedContentType.FEATURED_INDUSTRY).stream()
			.filter(document -> document.getId().equals(id))
			.map(document -> {
				FeaturedIndustryIngestion content = read(document, FeaturedIndustryIngestion.class);
				return new FeaturedIndustryResponse(document.getId(), content.getSector(), content.getSegment(),
					content.getTitle(), content.getRationale(), content.getPositiveScenario(),
					content.getNegativeScenario(), content.getValidFrom(), content.getValidTo(),
					content.getEvidence(), content.getCompanies());
			})
			.findFirst()
			.orElseThrow(() -> new ResourceNotFoundException("Featured industry " + id + " was not found."));
	}

	public List<ProcessedContentResponse> processedContents(ProcessedContentType type) {
		List<SummaryReferenceEntity> references = type == null
			? referenceRepository.findAllByCurrentTrueOrderByPeriodEndDesc()
			: referenceRepository.findBySummaryTypeAndCurrentTrueOrderByPeriodEndDesc(type);
		return hydrate(references).stream().map(this::processedResponse).toList();
	}

	public ProcessedContentResponse processedContent(String id) {
		SummaryReferenceEntity reference = referenceRepository.findById(id)
			.orElseThrow(() -> new ResourceNotFoundException("Processed content " + id + " was not found."));
		return processedResponse(requiredDocument(reference));
	}

	private List<SummaryDocument> latest(ProcessedContentType type) {
		return hydrate(referenceRepository.findBySummaryTypeAndCurrentTrueOrderByPeriodEndDesc(type));
	}

	private List<SummaryDocument> hydrate(List<SummaryReferenceEntity> references) {
		Map<String, SummaryDocument> documents = documentRepository
			.findAllById(references.stream().map(SummaryReferenceEntity::getId).toList()).stream()
			.collect(Collectors.toMap(SummaryDocument::getId, Function.identity()));
		return references.stream().map(reference -> {
			SummaryDocument document = documents.get(reference.getId());
			if (document == null) {
				throw new IllegalStateException("MongoDB summary " + reference.getId() + " is missing.");
			}
			return document;
		}).toList();
	}

	private SummaryDocument requiredDocument(SummaryReferenceEntity reference) {
		return documentRepository.findById(reference.getId())
			.orElseThrow(() -> new IllegalStateException("MongoDB summary " + reference.getId() + " is missing."));
	}

	private ProcessedContentResponse processedResponse(SummaryDocument document) {
		ProcessedContentType type = ProcessedContentType.valueOf(document.getSummaryType());
		return new ProcessedContentResponse(document.getId(), type, source(document), externalId(document),
			document.getRevision(), effectiveDate(document, type), publishedAt(document), collectedAt(document),
			document.getSources() == null ? java.util.Set.of()
				: java.util.Set.copyOf(document.getSources().getContentIds()),
			jsonNode(document));
	}

	private LocalDate effectiveDate(SummaryDocument document, ProcessedContentType type) {
		return switch (type) {
			case DAILY_BRIEFING -> read(document, DailyBriefingIngestion.class).getBriefingDate();
			case ECONOMY_OVERVIEW -> read(document, EconomyOverviewIngestion.class).getAsOfDate();
			case POLICY_BRIEFING -> read(document, PolicyBriefingIngestion.class).getPublishedAt()
				.atZone(KOREA).toLocalDate();
			case FEATURED_INDUSTRY -> read(document, FeaturedIndustryIngestion.class).getValidFrom();
		};
	}

	private String source(SummaryDocument document) {
		Object source = document.getPayload().get("source");
		return source == null ? null : source.toString();
	}

	private String externalId(SummaryDocument document) {
		Object externalId = document.getPayload().get("externalId");
		return externalId == null ? null : externalId.toString();
	}

	private java.time.Instant publishedAt(SummaryDocument document) {
		return switch (ProcessedContentType.valueOf(document.getSummaryType())) {
			case DAILY_BRIEFING -> read(document, DailyBriefingIngestion.class).getPublishedAt();
			case ECONOMY_OVERVIEW -> read(document, EconomyOverviewIngestion.class).getPublishedAt();
			case POLICY_BRIEFING -> read(document, PolicyBriefingIngestion.class).getPublishedAt();
			case FEATURED_INDUSTRY -> read(document, FeaturedIndustryIngestion.class).getCollectedAt();
		};
	}

	private java.time.Instant collectedAt(SummaryDocument document) {
		Object value = document.getPayload().get("collectedAt");
		return java.time.Instant.parse(value.toString());
	}

	private tools.jackson.databind.JsonNode jsonNode(SummaryDocument document) {
		try {
			return objectMapper.readTree(objectMapper.writeValueAsString(document.getPayload()));
		} catch (JacksonException exception) {
			throw new IllegalStateException("Stored summary payload is invalid: " + document.getId(), exception);
		}
	}

	private <T> T read(SummaryDocument document, Class<T> contentType) {
		try {
			return objectMapper.readValue(objectMapper.writeValueAsString(document.getPayload()), contentType);
		} catch (JacksonException exception) {
			throw new IllegalStateException("Stored content is invalid: " + document.getId(), exception);
		}
	}
}
