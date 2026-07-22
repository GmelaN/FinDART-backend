package com.jshyeon.findart.entity;

import java.time.Instant;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "original_contents")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class OriginalContent {
	@Id @Column(length = 36, updatable = false) private String id;
	@Enumerated(EnumType.STRING) @Column(name = "content_type", length = 40, nullable = false) private OriginalContentType contentType;
	@Column(nullable = false, length = 100) private String source;
	@Column(name = "external_id", nullable = false, length = 255) private String externalId;
	@Column(nullable = false) private int revision;
	@Column(nullable = false, length = 64) private String checksum;
	@Column(name = "source_url", nullable = false, length = 2048) private String sourceUrl;
	@Column(nullable = false, length = 1000) private String title;
	@Column(length = 255) private String publisher;
	@Column(nullable = false, length = 16) private String language;
	@Column(name = "raw_body", columnDefinition = "LONGTEXT") private String rawBody;
	@Column(name = "attributes_json", columnDefinition = "LONGTEXT") private String attributesJson;
	@Column(name = "published_at", nullable = false) private Instant publishedAt;
	@Column(name = "collected_at", nullable = false) private Instant collectedAt;

	public OriginalContent(OriginalContentType contentType, String source, String externalId, int revision, String checksum,
			String sourceUrl, String title, String publisher, String language, String rawBody, String attributesJson,
			Instant publishedAt, Instant collectedAt) {
		this.id = UUID.randomUUID().toString(); this.contentType = contentType; this.source = source; this.externalId = externalId;
		this.revision = revision; this.checksum = checksum; this.sourceUrl = sourceUrl; this.title = title;
		this.publisher = publisher; this.language = language; this.rawBody = rawBody; this.attributesJson = attributesJson;
		this.publishedAt = publishedAt; this.collectedAt = collectedAt;
	}
}
