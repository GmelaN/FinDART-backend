package com.jshyeon.findart.dto;

import java.time.Instant;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PolicyEvidence {
	@NotNull private DocumentType documentType;
	@NotBlank private String title;
	private String publisher;
	private Instant publishedAt;
	@NotBlank private String sourceUrl;
	public enum DocumentType { POLICY_BRIEFING, MINISTRY_PRESS_RELEASE, BUDGET, LEGISLATION, CORPORATE_INVESTMENT, OTHER }
}
