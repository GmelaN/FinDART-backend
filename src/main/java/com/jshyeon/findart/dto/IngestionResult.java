package com.jshyeon.findart.dto;

public record IngestionResult(String id, int revision, Status status) {
	public enum Status { CREATED, DUPLICATE, REVISED }
}
