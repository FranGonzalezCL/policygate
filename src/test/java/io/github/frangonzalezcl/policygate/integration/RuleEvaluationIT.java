package io.github.frangonzalezcl.policygate.integration;

import io.github.frangonzalezcl.policygate.api.dto.EvaluationResponse;
import io.github.frangonzalezcl.policygate.api.dto.PublishRuleRequest;
import io.github.frangonzalezcl.policygate.api.dto.RuleResponse;
import io.github.frangonzalezcl.policygate.support.AbstractIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

import static org.assertj.core.api.Assertions.assertThat;

// Level 2: evaluation, cache round-trip, direct Redis inspection, and re-evaluation after a new version.
class RuleEvaluationIT extends AbstractIntegrationTest {

	private void publish(String name, String expression) {
		restTemplate.postForEntity("/rules", new PublishRuleRequest(name, expression), RuleResponse.class);
	}

	private ResponseEntity<EvaluationResponse> evaluate(String name, Map<String, Object> context) {
		return restTemplate.postForEntity("/rules/" + name + "/evaluate", context, EvaluationResponse.class);
	}

	@Test
	void evaluatingAMatchingContextReturnsTrueAndANonMatchingContextReturnsFalse() {
		publish("eval_true_false", "amount > 100");

		assertThat(evaluate("eval_true_false", Map.of("amount", 500)).getBody().result()).isTrue();
		assertThat(evaluate("eval_true_false", Map.of("amount", 50)).getBody().result()).isFalse();
	}

	@Test
	void firstEvaluationIsUncachedAndTheIdenticalSecondCallIsServedFromCache() {
		publish("eval_cache_cycle", "amount > 100");

		EvaluationResponse first = evaluate("eval_cache_cycle", Map.of("amount", 500)).getBody();
		EvaluationResponse second = evaluate("eval_cache_cycle", Map.of("amount", 500)).getBody();

		assertThat(first.cached()).isFalse();
		assertThat(second.cached()).isTrue();
		assertThat(second.result()).isEqualTo(first.result());
	}

	@Test
	void redisHoldsTheCacheKeyInThePrescribedFormat() {
		publish("eval_redis_key", "amount > 100");
		evaluate("eval_redis_key", Map.of("amount", 500));

		Set<String> keys = redisTemplate.keys("rule:eval_redis_key:v1:*");

		assertThat(keys).hasSize(1);
		assertThat(keys.iterator().next()).matches(Pattern.compile("^rule:eval_redis_key:v1:[0-9a-f]{64}$"));
	}

	@Test
	void evaluationAfterPublishingANewVersionReflectsTheNewExpression() {
		publish("eval_new_version", "amount > 1000");
		ResponseEntity<EvaluationResponse> beforeUpgrade = evaluate("eval_new_version", Map.of("amount", 500));
		assertThat(beforeUpgrade.getBody().result()).isFalse();

		publish("eval_new_version", "amount > 100");
		ResponseEntity<EvaluationResponse> afterUpgrade = evaluate("eval_new_version", Map.of("amount", 500));

		assertThat(afterUpgrade.getBody().version()).isEqualTo(2);
		assertThat(afterUpgrade.getBody().result()).isTrue();
	}

	@Test
	void evaluatingAnUnknownNameReturnsNotFound() {
		ResponseEntity<Map> response = restTemplate.postForEntity(
				"/rules/eval_does_not_exist/evaluate", Map.of("amount", 1), Map.class);

		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
	}

}
