package io.github.frangonzalezcl.policygate.integration;

import io.github.frangonzalezcl.policygate.api.dto.PublishRuleRequest;
import io.github.frangonzalezcl.policygate.api.dto.RuleResponse;
import io.github.frangonzalezcl.policygate.repository.RuleRepository;
import io.github.frangonzalezcl.policygate.support.AbstractIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

// Level 2: publication rejections and the shape of the error body against RFC 7807.
class RuleValidationIT extends AbstractIntegrationTest {

	@Autowired
	private RuleRepository ruleRepository;

	@Test
	void syntacticallyInvalidExpressionIsRejectedAndPersistsNothing() {
		ResponseEntity<Map> response = restTemplate.postForEntity(
				"/rules", new PublishRuleRequest("val_bad_syntax", "amount >"), Map.class);

		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
		assertThat(ruleRepository.findAll().stream().anyMatch(r -> r.getName().equals("val_bad_syntax"))).isFalse();
	}

	@Test
	void expressionExceedingTheLengthLimitIsRejectedAndPersistsNothing() {
		String tooLong = "amount > 0".repeat(101);

		ResponseEntity<Map> response = restTemplate.postForEntity(
				"/rules", new PublishRuleRequest("val_too_long", tooLong), Map.class);

		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
		assertThat(ruleRepository.findAll().stream().anyMatch(r -> r.getName().equals("val_too_long"))).isFalse();
	}

	@Test
	void contextThatDoesNotSatisfyTheExpressionReturnsUnprocessableEntity() {
		restTemplate.postForEntity("/rules", new PublishRuleRequest("val_missing_prop", "missing > 1"), RuleResponse.class);

		ResponseEntity<Map> response = restTemplate.postForEntity(
				"/rules/val_missing_prop/evaluate", Map.of("amount", 1), Map.class);

		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNPROCESSABLE_CONTENT);
	}

	@Test
	void nonBooleanExpressionResultReturnsUnprocessableEntity() {
		restTemplate.postForEntity("/rules", new PublishRuleRequest("val_non_boolean", "amount * 2"), RuleResponse.class);

		ResponseEntity<Map> response = restTemplate.postForEntity(
				"/rules/val_non_boolean/evaluate", Map.of("amount", 5), Map.class);

		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNPROCESSABLE_CONTENT);
	}

	@Test
	void errorBodyMatchesTheProblemDetailShape() {
		ResponseEntity<Map> response = restTemplate.postForEntity(
				"/rules", new PublishRuleRequest("val_problem_shape", "amount >"), Map.class);

		assertThat(response.getHeaders().getContentType()).isEqualTo(MediaType.APPLICATION_PROBLEM_JSON);
		@SuppressWarnings("unchecked")
		Map<String, Object> body = (Map<String, Object>) response.getBody();
		assertThat(body).containsKeys("type", "title", "status", "detail");
		assertThat(body.get("status")).isEqualTo(400);
	}

}
