package com.jshyeon.findart.dto;

import java.time.LocalDate;
import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class FeaturedIndustryResponse {
	private String id;
	private String sector;
	private String segment;
	private String title;
	private String rationale;
	private String positiveScenario;
	private String negativeScenario;
	private LocalDate validFrom;
	private LocalDate validTo;
	private List<ContentReference> evidence;
	private List<FeaturedCompany> companies;
}
