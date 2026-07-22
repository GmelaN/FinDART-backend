package com.jshyeon.findart.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class MarketRegime {
	@NotNull private Category category;
	@NotBlank private String phase;
	@NotBlank private String rationale;
	public enum Category { INTEREST_RATE, EXCHANGE_RATE, INFLATION, GROWTH, EMPLOYMENT, TRADE }
}
