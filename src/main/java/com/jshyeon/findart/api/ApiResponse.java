package com.jshyeon.findart.api;

import java.time.Instant;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ApiResponse<T> {
	private boolean success;
	private T data;
	private ApiError error;
	private Instant timestamp;
	private String requestId;

	public static <T> ApiResponse<T> success(T data, String requestId) {
		return new ApiResponse<>(true, data, null, Instant.now(), requestId);
	}

	public static ApiResponse<Void> failure(ApiError error, String requestId) {
		return new ApiResponse<>(false, null, error, Instant.now(), requestId);
	}
}
