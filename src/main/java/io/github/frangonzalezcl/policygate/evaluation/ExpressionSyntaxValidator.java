package io.github.frangonzalezcl.policygate.evaluation;

import io.github.frangonzalezcl.policygate.exception.InvalidExpressionException;
import org.springframework.expression.Expression;
import org.springframework.expression.ParseException;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.stereotype.Component;

@Component
public class ExpressionSyntaxValidator {

	private final SpelExpressionParser parser = new SpelExpressionParser();

	public Expression parse(String expression) {
		try {
			return parser.parseExpression(expression);
		} catch (ParseException e) {
			throw new InvalidExpressionException(expression, e.getMessage(), e);
		}
	}

}
