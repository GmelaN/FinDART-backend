package com.jshyeon.findart.dto;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

public record FeaturedIndustryIngestion(
	@NotBlank String source, @NotBlank String externalId, @NotNull Instant collectedAt, String checksum,
	@NotEmpty List<@NotBlank String> originalContentIds,
	@NotBlank String sector, @NotBlank String segment, @NotBlank String title, @NotBlank String rationale,
	@NotBlank String positiveScenario, @NotBlank String negativeScenario, @NotNull LocalDate validFrom,
	LocalDate validTo, List<@Valid ContentReference> evidence, @NotEmpty List<@Valid FeaturedCompany> companies
) {
}
