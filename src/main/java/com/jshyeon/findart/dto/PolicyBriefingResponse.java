package com.jshyeon.findart.dto;

import java.time.Instant;
import java.util.List;

public record PolicyBriefingResponse(String id, String title, String body, Instant publishedAt,
	List<PolicyEvidence> evidence) {
}
