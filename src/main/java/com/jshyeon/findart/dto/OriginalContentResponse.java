package com.jshyeon.findart.dto;

import java.time.Instant;

import com.jshyeon.findart.entity.OriginalContentType;

import tools.jackson.databind.JsonNode;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class OriginalContentResponse {
	private String id;
	private OriginalContentType contentType;
	private String source;
	private String externalId;
	private int revision;
	private String sourceUrl;
	private String title;
	private String rawBody;
	private String publisher;
	private String language;
	private JsonNode attributes;
	private Instant publishedAt;
	private Instant collectedAt;
}
