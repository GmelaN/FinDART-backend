package com.jshyeon.findart.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record FeaturedCompany(@NotBlank String name, String ticker, @NotBlank String primaryBusiness,
	@NotNull CompanySize companySize, @NotNull OperatingProfitTrend operatingProfitTrend) {
	public enum CompanySize { LARGE_CAP, MID_CAP, SMALL_CAP, UNCLASSIFIED }
	public enum OperatingProfitTrend { IMPROVING, DETERIORATING, STABLE, VOLATILE, UNKNOWN }
}
