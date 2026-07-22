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
public class EconomyOverviewIngestion {
	@NotBlank private String source;
	@NotBlank private String externalId;
	@NotNull private Instant collectedAt;
	private String checksum;
	@NotEmpty private List<@NotBlank String> originalContentIds;
	@NotNull private LocalDate asOfDate;
	@NotEmpty private List<@Valid IndicatorCard> indicatorCards;
	private List<@Valid MarketEvent> scheduledEvents;
	@NotBlank private String abstractText;
	@NotNull private Instant publishedAt;
}
