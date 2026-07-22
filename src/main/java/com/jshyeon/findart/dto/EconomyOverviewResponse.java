package com.jshyeon.findart.dto;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

public record EconomyOverviewResponse(String id, LocalDate asOfDate, List<IndicatorCard> indicatorCards,
	List<MarketEvent> scheduledEvents, String abstractText, Instant publishedAt) {
}
