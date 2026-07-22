package com.jshyeon.findart.controller;

import java.time.ZoneId;

import com.jshyeon.findart.api.ApiResponse;
import com.jshyeon.findart.web.RequestIdFilter;
import com.jshyeon.findart.dto.DailyBriefingIngestion;
import com.jshyeon.findart.dto.EconomyOverviewIngestion;
import com.jshyeon.findart.dto.FeaturedIndustryIngestion;
import com.jshyeon.findart.dto.IngestionResult;
import com.jshyeon.findart.dto.PolicyBriefingIngestion;
import com.jshyeon.findart.service.ContentIngestionService;
import com.jshyeon.findart.entity.ProcessedContentType;

import jakarta.validation.Valid;
import jakarta.servlet.http.HttpServletRequest;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/collector/processed-contents")
@Tag(name = "Processed Content", description = "Collector-only processed-content ingestion endpoints")
@RequiredArgsConstructor
public class CollectorController {

	private static final ZoneId KOREA = ZoneId.of("Asia/Seoul");
	private final ContentIngestionService ingestionService;

	@PostMapping("/daily-briefings")
	public ResponseEntity<ApiResponse<IngestionResult>> dailyBriefing(@Valid @RequestBody DailyBriefingIngestion request, HttpServletRequest servletRequest) {
		return response(ingestionService.ingest(ProcessedContentType.DAILY_BRIEFING, request.getSource(), request.getExternalId(), request.getCollectedAt(),
			request.getBriefingDate(), request.getPublishedAt(), request.getOriginalContentIds(), request), servletRequest);
	}

	@PostMapping("/economy-overviews")
	public ResponseEntity<ApiResponse<IngestionResult>> economyOverview(@Valid @RequestBody EconomyOverviewIngestion request, HttpServletRequest servletRequest) {
		return response(ingestionService.ingest(ProcessedContentType.ECONOMY_OVERVIEW, request.getSource(), request.getExternalId(), request.getCollectedAt(),
			request.getAsOfDate(), request.getPublishedAt(), request.getOriginalContentIds(), request), servletRequest);
	}

	@PostMapping("/policy-briefings")
	public ResponseEntity<ApiResponse<IngestionResult>> policyBriefing(@Valid @RequestBody PolicyBriefingIngestion request, HttpServletRequest servletRequest) {
		return response(ingestionService.ingest(ProcessedContentType.POLICY_BRIEFING, request.getSource(), request.getExternalId(), request.getCollectedAt(),
			request.getPublishedAt().atZone(KOREA).toLocalDate(), request.getPublishedAt(), request.getOriginalContentIds(), request), servletRequest);
	}

	@PostMapping("/featured-industries")
	public ResponseEntity<ApiResponse<IngestionResult>> featuredIndustry(@Valid @RequestBody FeaturedIndustryIngestion request, HttpServletRequest servletRequest) {
		return response(ingestionService.ingest(ProcessedContentType.FEATURED_INDUSTRY, request.getSource(), request.getExternalId(), request.getCollectedAt(),
			request.getValidFrom(), request.getCollectedAt(), request.getOriginalContentIds(), request), servletRequest);
	}

	private ResponseEntity<ApiResponse<IngestionResult>> response(IngestionResult result, HttpServletRequest request) {
		HttpStatus status = result.getStatus() == IngestionResult.Status.CREATED ? HttpStatus.CREATED : HttpStatus.OK;
		return ResponseEntity.status(status).body(ApiResponse.success(result, RequestIdFilter.getRequestId(request)));
	}
}
