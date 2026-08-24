package io.github.frangonzalezcl.policygate.evaluation;

import io.github.frangonzalezcl.policygate.exception.ExpressionEvaluationException;
import io.github.frangonzalezcl.policygate.exception.NonBooleanResultException;
import org.junit.jupiter.api.Test;
import org.springframework.expression.Expression;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

// Level 1: no Spring context, no Docker, no database (AC-14).
class RestrictedSpelEvaluatorTest {

	private final ExpressionSyntaxValidator syntaxValidator = new ExpressionSyntaxValidator();
	private final RestrictedSpelEvaluator evaluator = new RestrictedSpelEvaluator();

	private boolean evaluate(String spel, Map<String, Object> context) {
		Expression expression = syntaxValidator.parse(spel);
		return evaluator.evaluate(expression, context);
	}

	@Test
	void trueBooleanExpressionOverContext() {
		boolean result = evaluate("amount > 100", Map.of("amount", 500));

		assertThat(result).isTrue();
	}

	@Test
	void falseBooleanExpressionOverContext() {
		boolean result = evaluate("amount > 100", Map.of("amount", 50));

		assertThat(result).isFalse();
	}

	@Test
	void logicalOperatorsAndComparisonsOverMultipleProperties() {
		boolean result = evaluate("amount > 100 and country == 'CL'", Map.of("amount", 500, "country", "CL"));

		assertThat(result).isTrue();
	}

	@Test
	void missingContextPropertyThrowsEvaluationException() {
		assertThatThrownBy(() -> evaluate("missing > 1", Map.of("amount", 1)))
				.isInstanceOf(ExpressionEvaluationException.class);
	}

	@Test
	void nonBooleanResultThrowsNonBooleanResultException() {
		assertThatThrownBy(() -> evaluate("amount * 2", Map.of("amount", 5)))
				.isInstanceOf(NonBooleanResultException.class);
	}

	@Test
	void typeReferenceIsRejectedByTheRestrictedContext() {
		assertThatThrownBy(() -> evaluate("T(java.lang.Runtime).getRuntime()", Map.of()))
				.isInstanceOf(ExpressionEvaluationException.class);
	}

	@Test
	void instanceMethodInvocationIsRejectedByTheRestrictedContext() {
		assertThatThrownBy(() -> evaluate("country.startsWith('C')", Map.of("country", "CL")))
				.isInstanceOf(ExpressionEvaluationException.class);
	}

}
