package com.jshyeon.findart.dto;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class TodayBriefingResponse {
	private String id;
	private LocalDate briefingDate;
	private DailyBriefingIngestion.Mode mode;
	private String title;
	private String summary;
	private List<MarketRegime> market;
	private List<ContentReference> headlines;
	private List<ContentReference> issues;
	private List<TrackedIssue> issueTracking;
	private List<MarketEvent> events;
	private Instant publishedAt;
}
