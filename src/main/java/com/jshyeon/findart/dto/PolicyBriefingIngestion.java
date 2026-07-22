package com.jshyeon.findart.dto;

import java.time.Instant;
import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

public record PolicyBriefingIngestion(
	@NotBlank String source, @NotBlank String externalId, @NotNull Instant collectedAt, String checksum,
	@NotEmpty List<@NotBlank String> originalContentIds,
	@NotBlank String title, @NotBlank String body, @NotNull Instant publishedAt,
	@NotEmpty List<@Valid PolicyEvidence> evidence
) {
}
