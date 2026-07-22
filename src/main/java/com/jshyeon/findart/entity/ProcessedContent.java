package com.jshyeon.findart.entity;

import java.time.Instant;
import java.time.LocalDate;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.UUID;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Table;

@Entity
@Table(name = "processed_contents")
public class ProcessedContent {
	@Id @Column(length = 36, updatable = false) private String id;
	@Enumerated(EnumType.STRING) @Column(name = "content_type", length = 40, nullable = false) private ProcessedContentType contentType;
	@Column(nullable = false, length = 100) private String source;
	@Column(name = "external_id", nullable = false, length = 255) private String externalId;
	@Column(nullable = false) private int revision;
	@Column(nullable = false, length = 64) private String checksum;
	@Column(name = "effective_date") private LocalDate effectiveDate;
	@Column(name = "published_at", nullable = false) private Instant publishedAt;
	@Column(name = "collected_at", nullable = false) private Instant collectedAt;
	@Column(nullable = false, columnDefinition = "LONGTEXT") private String payload;
	@ElementCollection
	@CollectionTable(name = "processed_content_originals", joinColumns = @JoinColumn(name = "processed_content_id"))
	@Column(name = "original_content_id", length = 36, nullable = false)
	private Set<String> originalContentIds = new LinkedHashSet<>();

	protected ProcessedContent() { }
	public ProcessedContent(ProcessedContentType contentType, String source, String externalId, int revision, String checksum,
			LocalDate effectiveDate, Instant publishedAt, Instant collectedAt, String payload, Set<String> originalContentIds) {
		this.id = UUID.randomUUID().toString(); this.contentType = contentType; this.source = source; this.externalId = externalId;
		this.revision = revision; this.checksum = checksum; this.effectiveDate = effectiveDate; this.publishedAt = publishedAt;
		this.collectedAt = collectedAt; this.payload = payload; this.originalContentIds = new LinkedHashSet<>(originalContentIds);
	}
	public String getId() { return id; } public ProcessedContentType getContentType() { return contentType; }
	public String getSource() { return source; } public String getExternalId() { return externalId; }
	public int getRevision() { return revision; } public String getChecksum() { return checksum; }
	public LocalDate getEffectiveDate() { return effectiveDate; } public Instant getPublishedAt() { return publishedAt; }
	public Instant getCollectedAt() { return collectedAt; } public String getPayload() { return payload; }
	public Set<String> getOriginalContentIds() { return Set.copyOf(originalContentIds); }
}
