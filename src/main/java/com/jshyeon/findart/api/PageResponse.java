package com.jshyeon.findart.api;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PageResponse<T> {
	private List<T> content;
	private int page;
	private int size;
	private long totalElements;
	private int totalPages;
	public static <T> PageResponse<T> of(List<T> all, int page, int size) {
		int from = Math.min(page * size, all.size());
		int to = Math.min(from + size, all.size());
		return new PageResponse<>(all.subList(from, to), page, size, all.size(), (int) Math.ceil((double) all.size() / size));
	}
}
