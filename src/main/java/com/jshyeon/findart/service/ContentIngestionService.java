package com.jshyeon.findart.service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.time.LocalDate;

import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;
import com.jshyeon.findart.dto.IngestionResult;
import com.jshyeon.findart.entity.ProcessedContent;
import com.jshyeon.findart.entity.ProcessedContentRepository;
import com.jshyeon.findart.entity.ProcessedContentType;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ContentIngestionService {

	private final ProcessedContentRepository repository;
	private final OriginalContentService originalContentService;
	private final ObjectMapper objectMapper;

	public ContentIngestionService(ProcessedContentRepository repository, OriginalContentService originalContentService, ObjectMapper objectMapper) {
		this.repository = repository;
		this.originalContentService = originalContentService;
		this.objectMapper = objectMapper;
	}

	@Transactional
	public IngestionResult ingest(ProcessedContentType type, String source, String externalId, Instant collectedAt,
			LocalDate effectiveDate, Instant publishedAt, java.util.List<String> originalContentIds, Object content) {
		if (originalContentIds.stream().anyMatch(id -> !originalContentService.exists(id))) {
			throw new IllegalArgumentException("Processed content references an unknown original content ID.");
		}
		String payload = serialize(content);
		String checksum = sha256(payload);
		var latest = repository.findFirstByContentTypeAndSourceAndExternalIdOrderByRevisionDesc(type, source, externalId);
		if (latest.isPresent() && latest.get().getChecksum().equals(checksum)) {
			ProcessedContent document = latest.get();
			return new IngestionResult(document.getId(), document.getRevision(), IngestionResult.Status.DUPLICATE);
		}

		int revision = latest.map(document -> document.getRevision() + 1).orElse(1);
		ProcessedContent document = repository.save(new ProcessedContent(type, source, externalId, revision, checksum,
			effectiveDate, publishedAt, collectedAt, payload, new java.util.LinkedHashSet<>(originalContentIds)));
		IngestionResult.Status status = revision == 1 ? IngestionResult.Status.CREATED : IngestionResult.Status.REVISED;
		return new IngestionResult(document.getId(), document.getRevision(), status);
	}

	private String serialize(Object content) {
		try {
			return objectMapper.writeValueAsString(content);
		} catch (JacksonException exception) {
			throw new IllegalStateException("Could not serialize collected content.", exception);
		}
	}

	private String sha256(String payload) {
		try {
			byte[] digest = MessageDigest.getInstance("SHA-256").digest(payload.getBytes(StandardCharsets.UTF_8));
			return java.util.HexFormat.of().formatHex(digest);
		} catch (NoSuchAlgorithmException exception) {
			throw new IllegalStateException("SHA-256 is unavailable.", exception);
		}
	}
}
