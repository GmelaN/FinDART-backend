package com.jshyeon.findart;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles("test")
@Import(MariaDbTestcontainersConfiguration.class)
class FindartApplicationTests {

	@Test
	void contextLoads() {
	}

}
