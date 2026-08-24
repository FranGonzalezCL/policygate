package io.github.frangonzalezcl.policygate.config;

import io.github.frangonzalezcl.policygate.evaluation.RestrictedSpelEvaluator;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.SerializationFeature;
import tools.jackson.databind.json.JsonMapper;

@Configuration
@EnableConfigurationProperties(CacheProperties.class)
public class EvaluationConfig {

	@Bean
	public RestrictedSpelEvaluator restrictedSpelEvaluator() {
		return new RestrictedSpelEvaluator();
	}

	@Bean
	public ObjectMapper canonicalContextObjectMapper() {
		return JsonMapper.builder()
				.enable(SerializationFeature.ORDER_MAP_ENTRIES_BY_KEYS)
				.build();
	}

}
