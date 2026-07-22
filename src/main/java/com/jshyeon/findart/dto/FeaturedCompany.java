package com.jshyeon.findart.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class FeaturedCompany {
	@NotBlank private String name;
	private String ticker;
	@NotBlank private String primaryBusiness;
	@NotNull private CompanySize companySize;
	@NotNull private OperatingProfitTrend operatingProfitTrend;
	public enum CompanySize { LARGE_CAP, MID_CAP, SMALL_CAP, UNCLASSIFIED }
	public enum OperatingProfitTrend { IMPROVING, DETERIORATING, STABLE, VOLATILE, UNKNOWN }
}
