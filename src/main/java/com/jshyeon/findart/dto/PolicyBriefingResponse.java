package com.jshyeon.findart.dto;

import java.time.Instant;
import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PolicyBriefingResponse {
	private String id;
	private String title;
	private String body;
	private Instant publishedAt;
	private List<PolicyEvidence> evidence;
}
