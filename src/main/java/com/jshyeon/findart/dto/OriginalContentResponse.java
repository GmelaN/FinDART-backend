package com.jshyeon.findart.dto;

import java.time.Instant;

import com.jshyeon.findart.entity.OriginalContentType;

import tools.jackson.databind.JsonNode;

public record OriginalContentResponse(String id, OriginalContentType contentType, String source, String externalId,
	int revision, String sourceUrl, String title, String rawBody, String publisher, String language,
	JsonNode attributes, Instant publishedAt, Instant collectedAt) { }
