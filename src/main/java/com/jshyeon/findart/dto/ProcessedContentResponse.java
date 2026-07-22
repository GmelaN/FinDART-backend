package com.jshyeon.findart.dto;

import java.time.Instant;
import java.time.LocalDate;
import java.util.Set;

import com.jshyeon.findart.entity.ProcessedContentType;

import tools.jackson.databind.JsonNode;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ProcessedContentResponse {
	private String id;
	private ProcessedContentType contentType;
	private String source;
	private String externalId;
	private int revision;
	private LocalDate effectiveDate;
	private Instant publishedAt;
	private Instant collectedAt;
	private Set<String> originalContentIds;
	private JsonNode content;
}
