package com.jshyeon.findart.controller;

import com.jshyeon.findart.api.ApiResponse;
import com.jshyeon.findart.api.PageResponse;
import com.jshyeon.findart.web.RequestIdFilter;
import com.jshyeon.findart.dto.IngestionResult;
import com.jshyeon.findart.dto.OriginalContentIngestion;
import com.jshyeon.findart.dto.OriginalContentResponse;
import com.jshyeon.findart.entity.OriginalContentType;
import com.jshyeon.findart.service.OriginalContentService;

import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1")
@Tag(name = "Original Content", description = "Raw source documents received from collectors")
public class OriginalContentController {
	private final OriginalContentService service;
	public OriginalContentController(OriginalContentService service) { this.service = service; }
	@PostMapping("/collector/original-contents")
	public ResponseEntity<ApiResponse<IngestionResult>> ingest(@Valid @RequestBody OriginalContentIngestion request, HttpServletRequest servletRequest) {
		IngestionResult result = service.ingest(request);
		return ResponseEntity.status(result.status() == IngestionResult.Status.CREATED ? HttpStatus.CREATED : HttpStatus.OK)
			.body(ApiResponse.success(result, RequestIdFilter.getRequestId(servletRequest)));
	}
	@GetMapping("/original-contents")
	public ApiResponse<PageResponse<OriginalContentResponse>> list(@RequestParam(required = false) OriginalContentType contentType,
			@RequestParam(defaultValue = "0") @Min(0) int page, @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size,
			HttpServletRequest request) {
		return ApiResponse.success(PageResponse.of(service.list(contentType), page, size), RequestIdFilter.getRequestId(request));
	}
	@GetMapping("/original-contents/{id}")
	public ApiResponse<OriginalContentResponse> get(@PathVariable String id, HttpServletRequest request) {
		return ApiResponse.success(service.get(id), RequestIdFilter.getRequestId(request));
	}
}
