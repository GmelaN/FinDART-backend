package com.jshyeon.findart.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record MarketRegime(@NotNull Category category, @NotBlank String phase, @NotBlank String rationale) {
	public enum Category { INTEREST_RATE, EXCHANGE_RATE, INFLATION, GROWTH, EMPLOYMENT, TRADE }
}
