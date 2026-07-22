package com.jshyeon.findart.entity;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

public interface ProcessedContentRepository extends JpaRepository<ProcessedContent, String> {
	Optional<ProcessedContent> findFirstByContentTypeAndSourceAndExternalIdOrderByRevisionDesc(ProcessedContentType type, String source, String externalId);
	List<ProcessedContent> findByContentTypeOrderByPublishedAtDesc(ProcessedContentType type);
	List<ProcessedContent> findByContentTypeAndEffectiveDateOrderByPublishedAtDesc(ProcessedContentType type, LocalDate effectiveDate);
	List<ProcessedContent> findByContentTypeAndEffectiveDateLessThanEqualOrderByEffectiveDateDescPublishedAtDesc(ProcessedContentType type, LocalDate effectiveDate);
	List<ProcessedContent> findAllByOrderByPublishedAtDesc();
}
