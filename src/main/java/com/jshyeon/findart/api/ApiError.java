package com.jshyeon.findart.api;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ApiError {
	private String code;
	private String message;
	private List<ApiFieldError> fieldErrors;
}
