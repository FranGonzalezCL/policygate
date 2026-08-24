package io.github.frangonzalezcl.policygate.cache;

import org.junit.jupiter.api.Test;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.SerializationFeature;
import tools.jackson.databind.json.JsonMapper;

import java.util.LinkedHashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

// Level 1: no Spring context, no Docker, no database (AC-14). Verifies AC-13 / SPEC-05-R39.
class ContextCanonicalizerTest {

	private static final ObjectMapper CANONICAL_MAPPER = JsonMapper.builder()
			.enable(SerializationFeature.ORDER_MAP_ENTRIES_BY_KEYS)
			.build();

	private final ContextCanonicalizer canonicalizer = new ContextCanonicalizer(CANONICAL_MAPPER);

	@Test
	void sameKeysAndValuesInDifferentOrderProduceTheSameHash() {
		Map<String, Object> first = new LinkedHashMap<>();
		first.put("amount", 500);
		first.put("country", "CL");

		Map<String, Object> second = new LinkedHashMap<>();
		second.put("country", "CL");
		second.put("amount", 500);

		assertThat(canonicalizer.hash(first)).isEqualTo(canonicalizer.hash(second));
	}

	@Test
	void differentValuesProduceDifferentHashes() {
		Map<String, Object> first = Map.of("amount", 500, "country", "CL");
		Map<String, Object> second = Map.of("amount", 501, "country", "CL");

		assertThat(canonicalizer.hash(first)).isNotEqualTo(canonicalizer.hash(second));
	}

}
