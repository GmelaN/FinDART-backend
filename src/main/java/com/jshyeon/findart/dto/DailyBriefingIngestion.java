package com.jshyeon.findart.dto;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class DailyBriefingIngestion {
	@NotBlank private String source;
	@NotBlank private String externalId;
	@NotNull private Instant collectedAt;
	private String checksum;
	@NotEmpty private List<@NotBlank String> originalContentIds;
	@NotNull private LocalDate briefingDate;
	@NotNull private Mode mode;
	@NotBlank private String title;
	@NotBlank private String summary;
	@NotEmpty private List<@Valid MarketRegime> market;
	private List<@Valid ContentReference> headlines;
	private List<@Valid ContentReference> issues;
	private List<@Valid TrackedIssue> issueTracking;
	private List<@Valid MarketEvent> events;
	@NotNull private Instant publishedAt;
	public enum Mode { DAILY, WEEKLY_RECAP, NEXT_WEEK_OUTLOOK }
}
