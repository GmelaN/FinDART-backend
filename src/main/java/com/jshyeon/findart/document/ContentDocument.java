package com.jshyeon.findart.document;

import java.time.Instant;
import java.util.Map;

import com.jshyeon.findart.entity.OriginalContentType;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Document(collection = "contents")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ContentDocument {

	@Id
	private String id;
	private Identity identity;
	private String title;
	private String sourceUrl;
	private String publisher;
	private String language;
	private Instant publishedAt;
	private Instant collectedAt;
	private Instant effectiveDate;
	private Body body;
	private Analysis analysis;
	private Processing processing;
	private Map<String, Object> attributes;
	private Boolean current;
	private Instant createdAt;
	private Instant updatedAt;

	public ContentDocument(String id, OriginalContentType contentType, String source, String externalId, int revision,
			String checksum, String title, String sourceUrl, String publisher, String language, String rawBody,
			Map<String, Object> attributes, Instant publishedAt, Instant collectedAt) {
		Instant now = Instant.now();
		this.id = id;
		this.identity = new Identity(contentType.name(), source, externalId, revision, checksum);
		this.title = title;
		this.sourceUrl = sourceUrl;
		this.publisher = publisher;
		this.language = language;
		this.publishedAt = publishedAt;
		this.collectedAt = collectedAt;
		this.body = new Body(rawBody, null, null, null, "INLINE", null);
		this.processing = new Processing("COLLECTED", null, null, null, null, null, null, null);
		this.attributes = attributes;
		this.current = true;
		this.createdAt = now;
		this.updatedAt = now;
	}

	public void markNotCurrent() {
		current = false;
		updatedAt = Instant.now();
	}

	public void markCurrent() {
		current = true;
		updatedAt = Instant.now();
	}

	@Getter
	@NoArgsConstructor(access = AccessLevel.PROTECTED)
	@AllArgsConstructor
	public static class Identity {
		private String contentType;
		private String source;
		private String externalId;
		private int revision;
		private String checksum;
	}

	@Getter
	@NoArgsConstructor(access = AccessLevel.PROTECTED)
	@AllArgsConstructor
	public static class Body {
		private String raw;
		private String normalized;
		private Integer rawTokenCount;
		private Integer normalizedTokenCount;
		private String storageType;
		private String storageUri;
	}

	@Getter
	@NoArgsConstructor(access = AccessLevel.PROTECTED)
	@AllArgsConstructor
	public static class Analysis {
		private String shortSummary;
		private String longSummary;
		private java.util.List<Map<String, Object>> categories;
		private java.util.List<Map<String, Object>> companies;
		private java.util.List<Map<String, Object>> sectors;
		private java.util.List<Map<String, Object>> keywords;
		private Double importanceScore;
		private Double marketImpactScore;
	}

	@Getter
	@NoArgsConstructor(access = AccessLevel.PROTECTED)
	@AllArgsConstructor
	public static class Processing {
		private String status;
		private String modelProvider;
		private String modelName;
		private String promptVersion;
		private String pipelineVersion;
		private Integer inputTokens;
		private Integer outputTokens;
		private String errorMessage;
	}
}
