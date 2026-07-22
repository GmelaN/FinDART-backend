package com.jshyeon.findart.controller;

import com.jshyeon.findart.api.ApiResponse;
import com.jshyeon.findart.api.PageResponse;
import com.jshyeon.findart.web.RequestIdFilter;
import com.jshyeon.findart.service.ContentQueryService;
import com.jshyeon.findart.dto.ProcessedContentResponse;
import com.jshyeon.findart.entity.ProcessedContentType;

import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/processed-contents")
@Tag(name = "Processed Content", description = "Frontend-ready content and its original-content provenance")
public class ProcessedContentController {
	private final ContentQueryService queryService;
	public ProcessedContentController(ContentQueryService queryService) { this.queryService = queryService; }
	@GetMapping
	public ApiResponse<PageResponse<ProcessedContentResponse>> list(@RequestParam(required = false) ProcessedContentType contentType,
			@RequestParam(defaultValue = "0") @Min(0) int page, @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size,
			HttpServletRequest request) {
		return ApiResponse.success(PageResponse.of(queryService.processedContents(contentType), page, size), RequestIdFilter.getRequestId(request));
	}
	@GetMapping("/{id}")
	public ApiResponse<ProcessedContentResponse> get(@PathVariable String id, HttpServletRequest request) {
		return ApiResponse.success(queryService.processedContent(id), RequestIdFilter.getRequestId(request));
	}
}
