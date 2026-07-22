package com.jshyeon.findart.api;

import java.time.Instant;

public record ApiResponse<T>(boolean success, T data, ApiError error, Instant timestamp, String requestId) {

	public static <T> ApiResponse<T> success(T data, String requestId) {
		return new ApiResponse<>(true, data, null, Instant.now(), requestId);
	}

	public static ApiResponse<Void> failure(ApiError error, String requestId) {
		return new ApiResponse<>(false, null, error, Instant.now(), requestId);
	}
}
