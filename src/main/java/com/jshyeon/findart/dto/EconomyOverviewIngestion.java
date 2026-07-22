package com.jshyeon.findart.dto;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

public record EconomyOverviewIngestion(
	@NotBlank String source, @NotBlank String externalId, @NotNull Instant collectedAt, String checksum,
	@NotEmpty List<@NotBlank String> originalContentIds,
	@NotNull LocalDate asOfDate, @NotEmpty List<@Valid IndicatorCard> indicatorCards,
	List<@Valid MarketEvent> scheduledEvents, @NotBlank String abstractText, @NotNull Instant publishedAt
) {
}
