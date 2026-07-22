package com.jshyeon.findart.dto;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class FeaturedIndustryIngestion {
	@NotBlank private String source;
	@NotBlank private String externalId;
	@NotNull private Instant collectedAt;
	private String checksum;
	@NotEmpty private List<@NotBlank String> originalContentIds;
	@NotBlank private String sector;
	@NotBlank private String segment;
	@NotBlank private String title;
	@NotBlank private String rationale;
	@NotBlank private String positiveScenario;
	@NotBlank private String negativeScenario;
	@NotNull private LocalDate validFrom;
	private LocalDate validTo;
	private List<@Valid ContentReference> evidence;
	@NotEmpty private List<@Valid FeaturedCompany> companies;
}
