package com.jshyeon.findart.dto;

import java.time.LocalDate;
import java.util.List;

public record FeaturedIndustryResponse(String id, String sector, String segment, String title, String rationale,
	String positiveScenario, String negativeScenario, LocalDate validFrom, LocalDate validTo,
	List<ContentReference> evidence, List<FeaturedCompany> companies) {
}
