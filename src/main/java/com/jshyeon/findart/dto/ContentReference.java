package com.jshyeon.findart.dto;

import jakarta.validation.constraints.NotBlank;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ContentReference {
	private String id;
	@NotBlank private String title;
	private String sourceUrl;
}
