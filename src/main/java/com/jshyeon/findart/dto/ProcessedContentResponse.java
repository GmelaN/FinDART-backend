package com.jshyeon.findart.dto;

import java.time.Instant;
import java.time.LocalDate;
import java.util.Set;

import com.jshyeon.findart.entity.ProcessedContentType;

import tools.jackson.databind.JsonNode;

public record ProcessedContentResponse(String id, ProcessedContentType contentType, String source, String externalId,
	int revision, LocalDate effectiveDate, Instant publishedAt, Instant collectedAt, Set<String> originalContentIds,
	JsonNode content) { }
