package com.jshyeon.findart.document;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import com.jshyeon.findart.entity.ProcessedContentType;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Document(collection = "summaries")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class SummaryDocument {

	@Id
	private String id;
	private String summaryType;
	private Scope scope;
	private Period period;
	private String title;
	private String summaryText;
	private Map<String, Object> payload;
	private Sources sources;
	private Processing processing;
	private int revision;
	private boolean current;
	private Instant createdAt;
	private Instant updatedAt;

	public SummaryDocument(String id, ProcessedContentType summaryType, String scopeKey, String timeGrain,
			Instant periodStart, Instant periodEnd, String title, String summaryText, Map<String, Object> payload,
			List<String> contentIds, int revision) {
		Instant now = Instant.now();
		this.id = id;
		this.summaryType = summaryType.name();
		this.scope = new Scope("GLOBAL", scopeKey, null, null, null, null, null);
		this.period = new Period(timeGrain, "FULL_PERIOD", periodStart, periodEnd);
		this.title = title;
		this.summaryText = summaryText;
		this.payload = payload;
		this.sources = new Sources(List.copyOf(contentIds), null, null);
		this.revision = revision;
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
	public static class Scope {
		private String type;
		private String key;
		private String companyId;
		private Long sectorId;
		private String issueId;
		private String categoryCode;
		private String economicDomain;
	}

	@Getter
	@NoArgsConstructor(access = AccessLevel.PROTECTED)
	@AllArgsConstructor
	public static class Period {
		private String type;
		private String windowCode;
		private Instant start;
		private Instant end;
	}

	@Getter
	@NoArgsConstructor(access = AccessLevel.PROTECTED)
	@AllArgsConstructor
	public static class Sources {
		private List<String> contentIds;
		private List<String> summaryIds;
		private List<String> metricCodes;
	}

	@Getter
	@NoArgsConstructor(access = AccessLevel.PROTECTED)
	@AllArgsConstructor
	public static class Processing {
		private String modelProvider;
		private String modelName;
		private String promptVersion;
		private String pipelineVersion;
		private Integer inputTokens;
		private Integer outputTokens;
	}
}
