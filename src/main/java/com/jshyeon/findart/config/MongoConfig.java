package com.jshyeon.findart.config;

import com.mongodb.MongoCredential;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.mongodb.autoconfigure.MongoClientSettingsBuilderCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.util.StringUtils;

@Configuration(proxyBeanMethods = false)
public class MongoConfig {

	@Bean
	@Order(Ordered.LOWEST_PRECEDENCE)
	MongoClientSettingsBuilderCustomizer mongoCredentialCustomizer(
			@Value("${spring.mongodb.username:}") String username,
			@Value("${spring.mongodb.password:}") String password,
			@Value("${spring.mongodb.authentication-database:findart}") String authenticationDatabase) {
		return builder -> {
			if (StringUtils.hasText(username)) {
				builder.credential(MongoCredential.createCredential(
					username, authenticationDatabase, password.toCharArray()));
			}
		};
	}
}
