package com.jshyeon.findart.dto;

import java.time.Instant;
import java.util.Map;

import com.jshyeon.findart.entity.OriginalContentType;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class OriginalContentIngestion {
	@NotNull private OriginalContentType contentType;
	@NotBlank private String source;
	@NotBlank private String externalId;
	@NotBlank private String sourceUrl;
	@NotBlank private String title;
	private String rawBody;
	private String publisher;
	private String language;
	private Map<String, Object> attributes;
	@NotNull private Instant publishedAt;
	@NotNull private Instant collectedAt;
}
