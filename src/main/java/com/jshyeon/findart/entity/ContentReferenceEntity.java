package com.jshyeon.findart.entity;

import java.time.Instant;

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
@Table(name = "content_references")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ContentReferenceEntity {

	@Id
	@Column(name = "content_id", length = 36, updatable = false)
	private String id;

	@Column(name = "mongo_database", length = 80, nullable = false)
	private String mongoDatabase;

	@Column(name = "mongo_collection", length = 80, nullable = false)
	private String mongoCollection;

	@Enumerated(EnumType.STRING)
	@Column(name = "content_type", length = 40, nullable = false)
	private OriginalContentType contentType;

	@Column(nullable = false, length = 100)
	private String source;

	@Column(name = "external_id", nullable = false, length = 255)
	private String externalId;

	@Column(nullable = false)
	private int revision;

	@Column(name = "published_at", nullable = false)
	private Instant publishedAt;

	@Column(name = "collected_at", nullable = false)
	private Instant collectedAt;

	@Column(name = "processing_status", nullable = false, length = 30)
	private String processingStatus;

	@Column(name = "analysis_version", length = 80)
	private String analysisVersion;

	public ContentReferenceEntity(String id, OriginalContentType contentType, String source, String externalId,
			int revision, Instant publishedAt, Instant collectedAt) {
		this.id = id;
		this.mongoDatabase = "findart";
		this.mongoCollection = "contents";
		this.contentType = contentType;
		this.source = source;
		this.externalId = externalId;
		this.revision = revision;
		this.publishedAt = publishedAt;
		this.collectedAt = collectedAt;
		this.processingStatus = "COLLECTED";
	}
}
