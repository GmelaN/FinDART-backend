package com.jshyeon.findart.controller;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;

import com.jshyeon.findart.api.ApiResponse;
import com.jshyeon.findart.api.PageResponse;
import com.jshyeon.findart.exception.ResourceNotFoundException;
import com.jshyeon.findart.web.RequestIdFilter;
import com.jshyeon.findart.dto.EconomyOverviewResponse;
import com.jshyeon.findart.dto.FeaturedIndustryResponse;
import com.jshyeon.findart.dto.PolicyBriefingResponse;
import com.jshyeon.findart.dto.TodayBriefingResponse;
import com.jshyeon.findart.service.ContentQueryService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.validation.annotation.Validated;

@RestController
@RequestMapping("/api/v1")
@Validated
@Tag(name = "Dashboard", description = "Market intelligence read endpoints")
public class DashboardController {

	private static final ZoneId KOREA = ZoneId.of("Asia/Seoul");
	private final ContentQueryService queryService;

	public DashboardController(ContentQueryService queryService) {
		this.queryService = queryService;
	}

	@GetMapping("/today")
	public ApiResponse<TodayBriefingResponse> today(@RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
			HttpServletRequest request) {
		return success(queryService.today(date), request);
	}

	@GetMapping("/economy-overview")
	public ApiResponse<EconomyOverviewResponse> economyOverview(
		@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate asOfDate,
		HttpServletRequest request) {
		return success(queryService.economyOverview(asOfDate == null ? LocalDate.now(KOREA) : asOfDate), request);
	}

	@GetMapping("/policy-briefings")
	public ApiResponse<PageResponse<PolicyBriefingResponse>> policyBriefings(
		@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
		@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
		@RequestParam(defaultValue = "0") @Min(0) int page,
		@RequestParam(defaultValue = "20") @Min(1) @Max(100) int size, HttpServletRequest request) {
		List<PolicyBriefingResponse> briefings = queryService.policyBriefings().stream()
			.filter(briefing -> from == null || !briefing.publishedAt().atZone(KOREA).toLocalDate().isBefore(from))
			.filter(briefing -> to == null || !briefing.publishedAt().atZone(KOREA).toLocalDate().isAfter(to))
			.toList();
		return success(PageResponse.of(briefings, page, size), request);
	}

	@GetMapping("/policy-briefings/{briefingId}")
	public ApiResponse<PolicyBriefingResponse> policyBriefing(@PathVariable String briefingId, HttpServletRequest request) {
		return success(queryService.policyBriefing(briefingId), request);
	}

	@GetMapping("/featured-industries")
	public ApiResponse<PageResponse<FeaturedIndustryResponse>> featuredIndustries(
		@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate asOfDate,
		@RequestParam(defaultValue = "0") @Min(0) int page,
		@RequestParam(defaultValue = "20") @Min(1) @Max(100) int size, HttpServletRequest request) {
		LocalDate date = asOfDate == null ? LocalDate.now(KOREA) : asOfDate;
		return success(PageResponse.of(queryService.featuredIndustries(date), page, size), request);
	}

	@GetMapping("/featured-industries/{industryId}")
	public ApiResponse<FeaturedIndustryResponse> featuredIndustry(@PathVariable String industryId, HttpServletRequest request) {
		return success(queryService.featuredIndustry(industryId), request);
	}

	@GetMapping({"/portfolio", "/portfolio/history"})
	public void portfolioDeferred() {
		throw new ResourceNotFoundException("Portfolio import and analysis are not implemented yet.");
	}

	private <T> ApiResponse<T> success(T data, HttpServletRequest request) {
		return ApiResponse.success(data, RequestIdFilter.getRequestId(request));
	}
}
