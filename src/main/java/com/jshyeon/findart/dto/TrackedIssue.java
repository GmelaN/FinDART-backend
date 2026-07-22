package com.jshyeon.findart.dto;

import java.util.List;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record TrackedIssue(@NotBlank String title, @NotNull List<@NotBlank String> developments) {
}
