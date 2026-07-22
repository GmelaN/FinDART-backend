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
public class EconomyOverviewResponse {
	private String id;
	private LocalDate asOfDate;
	private List<IndicatorCard> indicatorCards;
	private List<MarketEvent> scheduledEvents;
	private String abstractText;
	private Instant publishedAt;
}
