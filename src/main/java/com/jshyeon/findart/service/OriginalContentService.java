package com.jshyeon.findart.service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.List;

import com.jshyeon.findart.exception.ResourceNotFoundException;
import com.jshyeon.findart.dto.IngestionResult;
import com.jshyeon.findart.dto.OriginalContentIngestion;
import com.jshyeon.findart.dto.OriginalContentResponse;
import com.jshyeon.findart.entity.OriginalContent;
import com.jshyeon.findart.entity.OriginalContentRepository;
import com.jshyeon.findart.entity.OriginalContentType;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;

@Service
public class OriginalContentService {
	private final OriginalContentRepository repository;
	private final ObjectMapper objectMapper;
	public OriginalContentService(OriginalContentRepository repository, ObjectMapper objectMapper) { this.repository = repository; this.objectMapper = objectMapper; }

	@Transactional
	public IngestionResult ingest(OriginalContentIngestion request) {
		String canonical = json(request);
		String checksum = sha256(canonical);
		var latest = repository.findFirstByContentTypeAndSourceAndExternalIdOrderByRevisionDesc(request.contentType(), request.source(), request.externalId());
		if (latest.isPresent() && latest.get().getChecksum().equals(checksum)) {
			return new IngestionResult(latest.get().getId(), latest.get().getRevision(), IngestionResult.Status.DUPLICATE);
		}
		int revision = latest.map(value -> value.getRevision() + 1).orElse(1);
		OriginalContent stored = repository.save(new OriginalContent(request.contentType(), request.source(), request.externalId(), revision,
			checksum, request.sourceUrl(), request.title(), request.publisher(), request.language() == null ? "ko" : request.language(),
			request.rawBody(), json(request.attributes()), request.publishedAt(), request.collectedAt()));
		return new IngestionResult(stored.getId(), revision, revision == 1 ? IngestionResult.Status.CREATED : IngestionResult.Status.REVISED);
	}

	@Transactional(readOnly = true)
	public List<OriginalContentResponse> list(OriginalContentType type) {
		List<OriginalContent> contents = type == null ? repository.findAllByOrderByPublishedAtDesc() : repository.findByContentTypeOrderByPublishedAtDesc(type);
		return contents.stream().map(this::response).toList();
	}
	@Transactional(readOnly = true)
	public OriginalContentResponse get(String id) { return response(repository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Original content " + id + " was not found."))); }
	public boolean exists(String id) { return repository.existsById(id); }
	private OriginalContentResponse response(OriginalContent content) {
		try {
			return new OriginalContentResponse(content.getId(), content.getContentType(), content.getSource(), content.getExternalId(), content.getRevision(),
				content.getSourceUrl(), content.getTitle(), content.getRawBody(), content.getPublisher(), content.getLanguage(),
				content.getAttributesJson() == null ? null : objectMapper.readTree(content.getAttributesJson()), content.getPublishedAt(), content.getCollectedAt());
		} catch (JacksonException exception) { throw new IllegalStateException("Stored original content is invalid.", exception); }
	}
	private String json(Object value) { try { return objectMapper.writeValueAsString(value); } catch (JacksonException exception) { throw new IllegalStateException("Could not serialize original content.", exception); } }
	private String sha256(String value) { try { return java.util.HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8))); } catch (Exception exception) { throw new IllegalStateException(exception); } }
}
