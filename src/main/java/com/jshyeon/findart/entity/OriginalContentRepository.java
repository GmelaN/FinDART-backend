package com.jshyeon.findart.entity;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

public interface OriginalContentRepository extends JpaRepository<OriginalContent, String> {
	Optional<OriginalContent> findFirstByContentTypeAndSourceAndExternalIdOrderByRevisionDesc(OriginalContentType type, String source, String externalId);
	List<OriginalContent> findByContentTypeOrderByPublishedAtDesc(OriginalContentType type);
	List<OriginalContent> findAllByOrderByPublishedAtDesc();
}
