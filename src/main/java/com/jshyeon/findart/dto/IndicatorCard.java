package com.jshyeon.findart.dto;

import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class IndicatorCard {
	@NotNull private Indicator indicator;
	@NotNull private Double value;
	@NotBlank private String unit;
	@NotBlank private String period;
	private Double change;
	@NotNull private Direction direction;
	@NotNull private Horizon horizon;
	@NotBlank private String interpretation;
	private List<@Valid ContentReference> sourceReferences;
	public enum Indicator { INTEREST_RATE, INFLATION, EXCHANGE_RATE, GROWTH, EMPLOYMENT, EXPORTS, IMPORTS }
	public enum Direction { UP, DOWN, FLAT, MIXED }
	public enum Horizon { QUARTERLY, SEMIANNUAL }
}
