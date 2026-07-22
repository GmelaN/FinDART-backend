package com.jshyeon.findart.api;

import java.util.List;

public record PageResponse<T>(List<T> content, int page, int size, long totalElements, int totalPages) {
	public static <T> PageResponse<T> of(List<T> all, int page, int size) {
		int from = Math.min(page * size, all.size());
		int to = Math.min(from + size, all.size());
		return new PageResponse<>(all.subList(from, to), page, size, all.size(), (int) Math.ceil((double) all.size() / size));
	}
}
