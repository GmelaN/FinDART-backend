package com.jshyeon.findart.api;

import java.util.List;

public record ApiError(String code, String message, List<ApiFieldError> fieldErrors) {
}
