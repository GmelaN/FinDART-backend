package com.jshyeon.findart.dto;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

public record DailyBriefingIngestion(
	@NotBlank String source, @NotBlank String externalId, @NotNull Instant collectedAt, String checksum,
	@NotEmpty List<@NotBlank String> originalContentIds,
	@NotNull LocalDate briefingDate, @NotNull Mode mode, @NotBlank String title, @NotBlank String summary,
	@NotEmpty List<@Valid MarketRegime> market, List<@Valid ContentReference> headlines,
	List<@Valid ContentReference> issues, List<@Valid TrackedIssue> issueTracking, List<@Valid MarketEvent> events,
	@NotNull Instant publishedAt
) {
	public enum Mode { DAILY, WEEKLY_RECAP, NEXT_WEEK_OUTLOOK }
}
