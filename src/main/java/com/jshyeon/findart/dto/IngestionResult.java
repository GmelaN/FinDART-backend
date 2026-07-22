package com.jshyeon.findart.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class IngestionResult {
	private String id;
	private int revision;
	private Status status;
	public enum Status { CREATED, DUPLICATE, REVISED }
}
