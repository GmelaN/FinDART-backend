package com.jshyeon.findart.dto;

import java.time.Instant;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class MarketEvent {
	private String id;
	@NotBlank private String name;
	@NotNull private EventType eventType;
	@NotNull private Instant scheduledAt;
	private String summary;
	private String sourceUrl;
	public enum EventType { MONETARY_POLICY, IPO, OPTIONS_EXPIRY, ECONOMIC_RELEASE, GOVERNMENT, CORPORATE, OTHER }
}
