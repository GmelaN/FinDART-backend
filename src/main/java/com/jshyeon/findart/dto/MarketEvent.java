package com.jshyeon.findart.dto;

import java.time.Instant;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record MarketEvent(String id, @NotBlank String name, @NotNull EventType eventType, @NotNull Instant scheduledAt,
	String summary, String sourceUrl) {
	public enum EventType { MONETARY_POLICY, IPO, OPTIONS_EXPIRY, ECONOMIC_RELEASE, GOVERNMENT, CORPORATE, OTHER }
}
