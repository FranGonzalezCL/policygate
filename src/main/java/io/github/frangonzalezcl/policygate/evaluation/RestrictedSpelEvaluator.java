package io.github.frangonzalezcl.policygate.evaluation;

import io.github.frangonzalezcl.policygate.exception.ExpressionEvaluationException;
import io.github.frangonzalezcl.policygate.exception.NonBooleanResultException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.expression.EvaluationContext;
import org.springframework.expression.EvaluationException;
import org.springframework.expression.Expression;
import org.springframework.expression.spel.support.DataBindingPropertyAccessor;
import org.springframework.expression.spel.support.MapAccessor;
import org.springframework.expression.spel.support.SimpleEvaluationContext;

import java.util.Map;

public class RestrictedSpelEvaluator {

	private static final Logger log = LoggerFactory.getLogger(RestrictedSpelEvaluator.class);

	public boolean evaluate(Expression expression, Map<String, Object> context) {
		EvaluationContext evaluationContext = SimpleEvaluationContext
				.forPropertyAccessors(new MapAccessor(false), DataBindingPropertyAccessor.forReadOnlyAccess())
				.withAssignmentDisabled()
				.build();

		Object result;
		try {
			result = expression.getValue(evaluationContext, context);
		} catch (EvaluationException e) {
			log.warn("Evaluation failed for expression '{}': {}", expression.getExpressionString(), e.getMessage());
			throw new ExpressionEvaluationException(expression.getExpressionString(), e.getMessage(), e);
		}

		if (!(result instanceof Boolean booleanResult)) {
			log.warn("Expression '{}' produced a non-boolean result", expression.getExpressionString());
			throw new NonBooleanResultException(expression.getExpressionString(), result);
		}

		return booleanResult;
	}

}
