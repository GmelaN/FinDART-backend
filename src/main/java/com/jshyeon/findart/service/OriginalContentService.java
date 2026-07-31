package com.jshyeon.findart.service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.List;
import java.util.UUID;

import com.jshyeon.findart.document.ContentDocument;
import com.jshyeon.findart.document.ContentDocumentRepository;
import com.jshyeon.findart.dto.IngestionResult;
import com.jshyeon.findart.dto.OriginalContentIngestion;
import com.jshyeon.findart.dto.OriginalContentResponse;
import com.jshyeon.findart.entity.ContentReferenceEntity;
import com.jshyeon.findart.entity.ContentReferenceRepository;
import com.jshyeon.findart.entity.OriginalContentType;
import com.jshyeon.findart.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

@Service
@RequiredArgsConstructor
@Slf4j
public class OriginalContentService {

	private final ContentReferenceRepository referenceRepository;
	private final ContentDocumentRepository documentRepository;
	private final ObjectMapper objectMapper;

	@Transactional
	public IngestionResult ingest(OriginalContentIngestion request) {
		String checksum = sha256(json(request));
		var latestReference = referenceRepository.findFirstByContentTypeAndSourceAndExternalIdOrderByRevisionDesc(
			request.getContentType(), request.getSource(), request.getExternalId());
		ContentDocument previous = latestReference.map(this::requiredDocument).orElse(null);
		if (previous != null && previous.getIdentity().getChecksum().equals(checksum)) {
			return new IngestionResult(previous.getId(), previous.getIdentity().getRevision(), IngestionResult.Status.DUPLICATE);
		}

		int revision = latestReference.map(value -> value.getRevision() + 1).orElse(1);
		String id = UUID.randomUUID().toString();
		ContentDocument document = new ContentDocument(id, request.getContentType(), request.getSource(),
			request.getExternalId(), revision, checksum, request.getTitle(), request.getSourceUrl(),
			request.getPublisher(), request.getLanguage() == null ? "ko" : request.getLanguage(),
			request.getRawBody(), request.getAttributes(), request.getPublishedAt(), request.getCollectedAt());

		registerMongoRollbackCompensation(id, previous);
		documentRepository.insert(document);
		if (previous != null) {
			previous.markNotCurrent();
			documentRepository.save(previous);
		}

		referenceRepository.saveAndFlush(new ContentReferenceEntity(id, request.getContentType(), request.getSource(),
			request.getExternalId(), revision, request.getPublishedAt(), request.getCollectedAt()));
		return new IngestionResult(id, revision,
			revision == 1 ? IngestionResult.Status.CREATED : IngestionResult.Status.REVISED);
	}

	@Transactional(readOnly = true)
	public List<OriginalContentResponse> list(OriginalContentType type) {
		List<ContentReferenceEntity> references = type == null
			? referenceRepository.findAllByOrderByPublishedAtDesc()
			: referenceRepository.findByContentTypeOrderByPublishedAtDesc(type);
		return references.stream().map(this::requiredDocument).map(this::response).toList();
	}

	@Transactional(readOnly = true)
	public OriginalContentResponse get(String id) {
		ContentReferenceEntity reference = referenceRepository.findById(id)
			.orElseThrow(() -> new ResourceNotFoundException("Original content " + id + " was not found."));
		return response(requiredDocument(reference));
	}

	public boolean exists(String id) {
		return referenceRepository.existsById(id);
	}

	private ContentDocument requiredDocument(ContentReferenceEntity reference) {
		return documentRepository.findById(reference.getId())
			.orElseThrow(() -> new IllegalStateException("MongoDB content " + reference.getId() + " is missing."));
	}

	private OriginalContentResponse response(ContentDocument document) {
		ContentDocument.Identity identity = document.getIdentity();
		return new OriginalContentResponse(document.getId(), OriginalContentType.valueOf(identity.getContentType()),
			identity.getSource(), identity.getExternalId(), identity.getRevision(), document.getSourceUrl(),
			document.getTitle(), document.getBody() == null ? null : document.getBody().getRaw(),
			document.getPublisher(), document.getLanguage(), jsonNode(document.getAttributes()),
			document.getPublishedAt(), document.getCollectedAt());
	}

	private void registerMongoRollbackCompensation(String newDocumentId, ContentDocument previous) {
		if (!TransactionSynchronizationManager.isSynchronizationActive()) {
			throw new IllegalStateException("A MariaDB transaction is required before writing MongoDB content.");
		}
		TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
			@Override
			public void afterCompletion(int status) {
				if (status != STATUS_COMMITTED) {
					try {
						documentRepository.deleteById(newDocumentId);
					} catch (RuntimeException exception) {
						log.error("Could not remove rolled-back MongoDB content {}.", newDocumentId, exception);
					}
					if (previous != null) {
						try {
							previous.markCurrent();
							documentRepository.save(previous);
						} catch (RuntimeException exception) {
							log.error("Could not restore MongoDB content revision {}.", previous.getId(), exception);
						}
					}
				}
			}
		});
	}

	private JsonNode jsonNode(Object value) {
		if (value == null) {
			return null;
		}
		try {
			return objectMapper.readTree(objectMapper.writeValueAsString(value));
		} catch (JacksonException exception) {
			throw new IllegalStateException("Stored original content is invalid.", exception);
		}
	}

	private String json(Object value) {
		try {
			return objectMapper.writeValueAsString(value);
		} catch (JacksonException exception) {
			throw new IllegalStateException("Could not serialize original content.", exception);
		}
	}

	private String sha256(String value) {
		try {
			return java.util.HexFormat.of().formatHex(
				MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8)));
		} catch (Exception exception) {
			throw new IllegalStateException(exception);
		}
	}
}
