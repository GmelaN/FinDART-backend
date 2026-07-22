package com.jshyeon.findart.dto;

import jakarta.validation.constraints.NotBlank;

public record ContentReference(String id, @NotBlank String title, String sourceUrl) {
}
