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
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "processed_contents")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
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

	public ProcessedContent(ProcessedContentType contentType, String source, String externalId, int revision, String checksum,
			LocalDate effectiveDate, Instant publishedAt, Instant collectedAt, String payload, Set<String> originalContentIds) {
		this.id = UUID.randomUUID().toString(); this.contentType = contentType; this.source = source; this.externalId = externalId;
		this.revision = revision; this.checksum = checksum; this.effectiveDate = effectiveDate; this.publishedAt = publishedAt;
		this.collectedAt = collectedAt; this.payload = payload; this.originalContentIds = new LinkedHashSet<>(originalContentIds);
	}
	public Set<String> getOriginalContentIds() { return Set.copyOf(originalContentIds); }
}
