package com.jshyeon.findart.exception;

import java.util.List;

import com.jshyeon.findart.api.ApiError;
import com.jshyeon.findart.api.ApiFieldError;
import com.jshyeon.findart.api.ApiResponse;
import com.jshyeon.findart.web.RequestIdFilter;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

	@ExceptionHandler(ResourceNotFoundException.class)
	ResponseEntity<ApiResponse<Void>> handleNotFound(ResourceNotFoundException exception, HttpServletRequest request) {
		return error(HttpStatus.NOT_FOUND, "NOT_FOUND", exception.getMessage(), List.of(), request);
	}

	@ExceptionHandler(MethodArgumentNotValidException.class)
	ResponseEntity<ApiResponse<Void>> handleValidation(MethodArgumentNotValidException exception, HttpServletRequest request) {
		List<ApiFieldError> fieldErrors = exception.getBindingResult().getFieldErrors().stream()
			.map(error -> new ApiFieldError(error.getField(), error.getDefaultMessage()))
			.toList();
		return error(HttpStatus.BAD_REQUEST, "VALIDATION_FAILED", "Request validation failed.", fieldErrors, request);
	}

	@ExceptionHandler(ConstraintViolationException.class)
	ResponseEntity<ApiResponse<Void>> handleConstraintViolation(ConstraintViolationException exception, HttpServletRequest request) {
		List<ApiFieldError> fieldErrors = exception.getConstraintViolations().stream()
			.map(error -> new ApiFieldError(error.getPropertyPath().toString(), error.getMessage()))
			.toList();
		return error(HttpStatus.BAD_REQUEST, "VALIDATION_FAILED", "Request validation failed.", fieldErrors, request);
	}

	@ExceptionHandler(HttpMessageNotReadableException.class)
	ResponseEntity<ApiResponse<Void>> handleUnreadableBody(HttpMessageNotReadableException exception, HttpServletRequest request) {
		return error(HttpStatus.BAD_REQUEST, "VALIDATION_FAILED", "Request body is invalid.", List.of(), request);
	}

	@ExceptionHandler(IllegalArgumentException.class)
	ResponseEntity<ApiResponse<Void>> handleInvalidArgument(IllegalArgumentException exception, HttpServletRequest request) {
		return error(HttpStatus.BAD_REQUEST, "VALIDATION_FAILED", exception.getMessage(), List.of(), request);
	}

	@ExceptionHandler(Exception.class)
	ResponseEntity<ApiResponse<Void>> handleUnexpected(Exception exception, HttpServletRequest request) {
		return error(HttpStatus.INTERNAL_SERVER_ERROR, "INTERNAL_ERROR", "An unexpected error occurred.", List.of(), request);
	}

	private ResponseEntity<ApiResponse<Void>> error(HttpStatus status, String code, String message,
			List<ApiFieldError> fieldErrors, HttpServletRequest request) {
		return ResponseEntity.status(status)
			.body(ApiResponse.failure(new ApiError(code, message, fieldErrors.isEmpty() ? null : fieldErrors),
				RequestIdFilter.getRequestId(request)));
	}
}
