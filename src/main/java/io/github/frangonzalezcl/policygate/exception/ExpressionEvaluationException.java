package io.github.frangonzalezcl.policygate.exception;

public class ExpressionEvaluationException extends RuntimeException {

	private final String expression;

	public ExpressionEvaluationException(String expression, String reason, Throwable cause) {
		super("Expression '%s' could not be evaluated against the given context: %s".formatted(expression, reason), cause);
		this.expression = expression;
	}

	public String getExpression() {
		return expression;
	}

}
