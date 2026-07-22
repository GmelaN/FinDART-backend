package com.jshyeon.findart.dto;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

public record TodayBriefingResponse(
	String id, LocalDate briefingDate, DailyBriefingIngestion.Mode mode, String title, String summary,
	List<MarketRegime> market, List<ContentReference> headlines, List<ContentReference> issues,
	List<TrackedIssue> issueTracking, List<MarketEvent> events, Instant publishedAt
) {
}
