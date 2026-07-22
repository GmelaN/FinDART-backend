package com.jshyeon.findart.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

import com.jshyeon.findart.dto.DailyBriefingIngestion;
import com.jshyeon.findart.dto.MarketRegime;
import com.jshyeon.findart.dto.OriginalContentIngestion;
import com.jshyeon.findart.entity.OriginalContentType;
import com.jshyeon.findart.MariaDbTestcontainersConfiguration;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.ObjectMapper;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@ActiveProfiles("test")
@Import(MariaDbTestcontainersConfiguration.class)
class CollectorControllerIntegrationTests {

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private ObjectMapper objectMapper;

	@Test
	void storesAndReturnsDailyBriefing() throws Exception {
		LocalDate date = LocalDate.of(2026, 7, 20);
		OriginalContentIngestion original = new OriginalContentIngestion(OriginalContentType.ARTICLE, "test-source", "article-2026-07-20",
			"https://example.com/article", "Original article", "Original source body", "Example", "ko", java.util.Map.of(),
			Instant.parse("2026-07-20T00:00:00Z"), Instant.parse("2026-07-20T00:00:00Z"));
		String originalId = objectMapper.readTree(mockMvc.perform(post("/api/v1/collector/original-contents")
				.contentType(MediaType.APPLICATION_JSON).content(objectMapper.writeValueAsString(original)))
			.andExpect(status().isCreated()).andReturn().getResponse().getContentAsString()).get("data").get("id").asString();
		DailyBriefingIngestion request = new DailyBriefingIngestion("test-collector", "today-2026-07-20", Instant.parse("2026-07-20T00:00:00Z"),
			null, List.of(originalId), date, DailyBriefingIngestion.Mode.DAILY, "Today", "Market summary",
			List.of(new MarketRegime(MarketRegime.Category.INTEREST_RATE, "EASING_EXPECTATION", "Inflation slowed.")),
			List.of(), List.of(), List.of(), List.of(), Instant.parse("2026-07-20T00:00:00Z"));

		String processedId = objectMapper.readTree(mockMvc.perform(post("/api/v1/collector/processed-contents/daily-briefings")
				.contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(request)))
			.andExpect(status().isCreated())
			.andExpect(jsonPath("$.success").value(true))
			.andExpect(jsonPath("$.data.status").value("CREATED"))
			.andExpect(jsonPath("$.requestId").exists()).andReturn().getResponse().getContentAsString()).get("data").get("id").asString();

		mockMvc.perform(get("/api/v1/original-contents/{id}", originalId))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.data.rawBody").value("Original source body"));
		mockMvc.perform(get("/api/v1/processed-contents/{id}", processedId))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.data.originalContentIds[0]").value(originalId));

		mockMvc.perform(get("/api/v1/today").param("date", date.toString()))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.success").value(true))
			.andExpect(jsonPath("$.data.title").value("Today"))
			.andExpect(jsonPath("$.data.mode").value("DAILY"));
	}

	@Test
	void returnsCommonErrorEnvelopeAndOpenApiDocument() throws Exception {
		mockMvc.perform(get("/api/v1/today").param("date", "2099-01-01").header("X-Request-Id", "test-request-id"))
			.andExpect(status().isNotFound())
			.andExpect(jsonPath("$.success").value(false))
			.andExpect(jsonPath("$.data").doesNotExist())
			.andExpect(jsonPath("$.error.code").value("NOT_FOUND"))
			.andExpect(jsonPath("$.requestId").value("test-request-id"));

		mockMvc.perform(get("/v3/api-docs"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.info.title").value("FinDART API"))
			.andExpect(jsonPath("$.paths['/api/v1/today']").exists());
	}
}
