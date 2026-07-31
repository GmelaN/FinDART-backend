package com.jshyeon.findart.entity;

import java.time.Instant;
import java.util.LinkedHashSet;
import java.util.Set;

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
@Table(name = "summary_references")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class SummaryReferenceEntity {

	@Id
	@Column(name = "summary_id", length = 36, updatable = false)
	private String id;

	@Column(name = "mongo_database", length = 80, nullable = false)
	private String mongoDatabase;

	@Column(name = "mongo_collection", length = 80, nullable = false)
	private String mongoCollection;

	@Enumerated(EnumType.STRING)
	@Column(name = "summary_type", length = 60, nullable = false)
	private ProcessedContentType summaryType;

	@Column(name = "scope_type", length = 30, nullable = false)
	private String scopeType;

	@Column(name = "scope_key", length = 180, nullable = false)
	private String scopeKey;

	@Column(name = "time_grain", length = 20, nullable = false)
	private String timeGrain;

	@Column(name = "window_code", length = 40, nullable = false)
	private String windowCode;

	@Column(name = "period_start", nullable = false)
	private Instant periodStart;

	@Column(name = "period_end", nullable = false)
	private Instant periodEnd;

	@Column(name = "source_cutoff_at", nullable = false)
	private Instant sourceCutoffAt;

	@Column(nullable = false)
	private int revision;

	@Column(name = "is_current", nullable = false)
	private boolean current;

	@ElementCollection
	@CollectionTable(name = "summary_content_links", joinColumns = @JoinColumn(name = "summary_id"))
	@Column(name = "content_id", length = 36, nullable = false)
	private Set<String> contentIds = new LinkedHashSet<>();

	public SummaryReferenceEntity(String id, ProcessedContentType summaryType, String scopeKey, String timeGrain,
			Instant periodStart, Instant periodEnd, Instant sourceCutoffAt, int revision, Set<String> contentIds) {
		this.id = id;
		this.mongoDatabase = "findart";
		this.mongoCollection = "summaries";
		this.summaryType = summaryType;
		this.scopeType = "GLOBAL";
		this.scopeKey = scopeKey;
		this.timeGrain = timeGrain;
		this.windowCode = "FULL_PERIOD";
		this.periodStart = periodStart;
		this.periodEnd = periodEnd;
		this.sourceCutoffAt = sourceCutoffAt;
		this.revision = revision;
		this.current = true;
		this.contentIds = new LinkedHashSet<>(contentIds);
	}

	public Set<String> getContentIds() {
		return Set.copyOf(contentIds);
	}

	public void markNotCurrent() {
		current = false;
	}

	public void markCurrent() {
		current = true;
	}
}
