package com.jshyeon.findart.dto;

import java.time.Instant;
import java.util.Map;

import com.jshyeon.findart.entity.OriginalContentType;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record OriginalContentIngestion(
	@NotNull OriginalContentType contentType, @NotBlank String source, @NotBlank String externalId,
	@NotBlank String sourceUrl, @NotBlank String title, String rawBody, String publisher, String language,
	Map<String, Object> attributes, @NotNull Instant publishedAt, @NotNull Instant collectedAt
) { }
