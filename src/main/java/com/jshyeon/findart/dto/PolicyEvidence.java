package com.jshyeon.findart.dto;

import java.time.Instant;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record PolicyEvidence(@NotNull DocumentType documentType, @NotBlank String title, String publisher,
	Instant publishedAt, @NotBlank String sourceUrl) {
	public enum DocumentType { POLICY_BRIEFING, MINISTRY_PRESS_RELEASE, BUDGET, LEGISLATION, CORPORATE_INVESTMENT, OTHER }
}
