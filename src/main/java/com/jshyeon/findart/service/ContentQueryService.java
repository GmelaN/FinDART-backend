package com.jshyeon.findart.service;

import java.time.LocalDate;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;
import com.jshyeon.findart.dto.DailyBriefingIngestion;
import com.jshyeon.findart.dto.EconomyOverviewIngestion;
import com.jshyeon.findart.dto.EconomyOverviewResponse;
import com.jshyeon.findart.dto.FeaturedIndustryIngestion;
import com.jshyeon.findart.dto.FeaturedIndustryResponse;
import com.jshyeon.findart.dto.PolicyBriefingIngestion;
import com.jshyeon.findart.dto.PolicyBriefingResponse;
import com.jshyeon.findart.dto.TodayBriefingResponse;
import com.jshyeon.findart.dto.ProcessedContentResponse;
import com.jshyeon.findart.entity.ProcessedContent;
import com.jshyeon.findart.entity.ProcessedContentRepository;
import com.jshyeon.findart.entity.ProcessedContentType;
import com.jshyeon.findart.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class ContentQueryService {

	private final ProcessedContentRepository repository;
	private final ObjectMapper objectMapper;

	public TodayBriefingResponse today(LocalDate date) {
		DailyBriefingIngestion.Mode mode = switch (date.getDayOfWeek()) {
			case SATURDAY -> DailyBriefingIngestion.Mode.WEEKLY_RECAP;
			case SUNDAY -> DailyBriefingIngestion.Mode.NEXT_WEEK_OUTLOOK;
			default -> DailyBriefingIngestion.Mode.DAILY;
		};
		return repository.findByContentTypeAndEffectiveDateOrderByPublishedAtDesc(ProcessedContentType.DAILY_BRIEFING, date).stream()
			.map(document -> Map.entry(document, read(document, DailyBriefingIngestion.class)))
			.filter(entry -> entry.getValue().getMode() == mode)
			.findFirst()
			.map(entry -> {
				DailyBriefingIngestion content = entry.getValue();
				return new TodayBriefingResponse(entry.getKey().getId(), content.getBriefingDate(), content.getMode(), content.getTitle(),
					content.getSummary(), content.getMarket(), content.getHeadlines(), content.getIssues(), content.getIssueTracking(),
					content.getEvents(), content.getPublishedAt());
			})
			.orElseThrow(() -> new ResourceNotFoundException("No briefing is available for " + date + "."));
	}

	public EconomyOverviewResponse economyOverview(LocalDate asOfDate) {
		ProcessedContent document = repository
			.findByContentTypeAndEffectiveDateLessThanEqualOrderByEffectiveDateDescPublishedAtDesc(ProcessedContentType.ECONOMY_OVERVIEW, asOfDate)
			.stream().findFirst()
			.orElseThrow(() -> new ResourceNotFoundException("No economy overview is available on or before " + asOfDate + "."));
		EconomyOverviewIngestion content = read(document, EconomyOverviewIngestion.class);
		return new EconomyOverviewResponse(document.getId(), content.getAsOfDate(), content.getIndicatorCards(), content.getScheduledEvents(),
			content.getAbstractText(), content.getPublishedAt());
	}

	public List<PolicyBriefingResponse> policyBriefings() {
		return latest(ProcessedContentType.POLICY_BRIEFING).stream()
			.map(document -> {
				PolicyBriefingIngestion content = read(document, PolicyBriefingIngestion.class);
				return new PolicyBriefingResponse(document.getId(), content.getTitle(), content.getBody(), content.getPublishedAt(), content.getEvidence());
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
				return new FeaturedIndustryResponse(document.getId(), content.getSector(), content.getSegment(), content.getTitle(), content.getRationale(),
					content.getPositiveScenario(), content.getNegativeScenario(), content.getValidFrom(), content.getValidTo(), content.getEvidence(), content.getCompanies());
			})
			.filter(industry -> !industry.getValidFrom().isAfter(asOfDate)
				&& (industry.getValidTo() == null || !industry.getValidTo().isBefore(asOfDate)))
			.toList();
	}

	public FeaturedIndustryResponse featuredIndustry(String id) {
		return latest(ProcessedContentType.FEATURED_INDUSTRY).stream().filter(document -> document.getId().equals(id)).findFirst()
			.map(document -> {
				FeaturedIndustryIngestion content = read(document, FeaturedIndustryIngestion.class);
				return new FeaturedIndustryResponse(document.getId(), content.getSector(), content.getSegment(), content.getTitle(), content.getRationale(),
					content.getPositiveScenario(), content.getNegativeScenario(), content.getValidFrom(), content.getValidTo(), content.getEvidence(), content.getCompanies());
			})
			.orElseThrow(() -> new ResourceNotFoundException("Featured industry " + id + " was not found."));
	}

	public List<ProcessedContentResponse> processedContents(ProcessedContentType type) {
		List<ProcessedContent> documents = type == null ? repository.findAllByOrderByPublishedAtDesc() : latest(type);
		return documents.stream().map(this::processedResponse).toList();
	}

	public ProcessedContentResponse processedContent(String id) {
		return processedResponse(repository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Processed content " + id + " was not found.")));
	}

	private List<ProcessedContent> latest(ProcessedContentType type) {
		Map<String, ProcessedContent> latest = new LinkedHashMap<>();
		for (ProcessedContent document : repository.findByContentTypeOrderByPublishedAtDesc(type)) {
			String key = document.getSource() + '\u0000' + document.getExternalId();
			latest.merge(key, document, (first, second) -> first.getRevision() >= second.getRevision() ? first : second);
		}
		return List.copyOf(latest.values());
	}

	private ProcessedContentResponse processedResponse(ProcessedContent document) {
		try {
			return new ProcessedContentResponse(document.getId(), document.getContentType(), document.getSource(), document.getExternalId(),
				document.getRevision(), document.getEffectiveDate(), document.getPublishedAt(), document.getCollectedAt(),
				document.getOriginalContentIds(), objectMapper.readTree(document.getPayload()));
		} catch (JacksonException exception) { throw new IllegalStateException("Stored processed content is invalid.", exception); }
	}

	private <T> T read(ProcessedContent document, Class<T> contentType) {
		try {
			return objectMapper.readValue(document.getPayload(), contentType);
		} catch (JacksonException exception) {
			throw new IllegalStateException("Stored content is invalid: " + document.getId(), exception);
		}
	}
}
