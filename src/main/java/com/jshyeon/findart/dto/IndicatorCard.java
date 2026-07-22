package com.jshyeon.findart.dto;

import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record IndicatorCard(
	@NotNull Indicator indicator, @NotNull Double value, @NotBlank String unit, @NotBlank String period,
	Double change, @NotNull Direction direction, @NotNull Horizon horizon, @NotBlank String interpretation,
	List<@Valid ContentReference> sourceReferences
) {
	public enum Indicator { INTEREST_RATE, INFLATION, EXCHANGE_RATE, GROWTH, EMPLOYMENT, EXPORTS, IMPORTS }
	public enum Direction { UP, DOWN, FLAT, MIXED }
	public enum Horizon { QUARTERLY, SEMIANNUAL }
}
