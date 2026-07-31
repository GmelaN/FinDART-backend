package com.jshyeon.findart.entity;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

public interface ContentReferenceRepository extends JpaRepository<ContentReferenceEntity, String> {
	Optional<ContentReferenceEntity> findFirstByContentTypeAndSourceAndExternalIdOrderByRevisionDesc(
		OriginalContentType type, String source, String externalId);
	List<ContentReferenceEntity> findByContentTypeOrderByPublishedAtDesc(OriginalContentType type);
	List<ContentReferenceEntity> findAllByOrderByPublishedAtDesc();
}
