package com.jshyeon.findart.dto;

import java.time.Instant;
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
public class PolicyBriefingIngestion {
	@NotBlank private String source;
	@NotBlank private String externalId;
	@NotNull private Instant collectedAt;
	private String checksum;
	@NotEmpty private List<@NotBlank String> originalContentIds;
	@NotBlank private String title;
	@NotBlank private String body;
	@NotNull private Instant publishedAt;
	@NotEmpty private List<@Valid PolicyEvidence> evidence;
}
