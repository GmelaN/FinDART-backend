package com.jshyeon.findart.entity;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

public interface SummaryReferenceRepository extends JpaRepository<SummaryReferenceEntity, String> {
	Optional<SummaryReferenceEntity> findFirstBySummaryTypeAndScopeKeyOrderByRevisionDesc(
		ProcessedContentType type, String scopeKey);
	List<SummaryReferenceEntity> findBySummaryTypeAndCurrentTrueOrderByPeriodEndDesc(ProcessedContentType type);
	List<SummaryReferenceEntity> findAllByCurrentTrueOrderByPeriodEndDesc();
}
