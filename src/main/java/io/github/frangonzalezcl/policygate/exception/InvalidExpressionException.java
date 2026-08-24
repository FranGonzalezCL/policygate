package io.github.frangonzalezcl.policygate.exception;

public class InvalidExpressionException extends RuntimeException {

	private final String expression;

	public InvalidExpressionException(String expression, String reason, Throwable cause) {
		super("Expression '%s' is not valid SpEL: %s".formatted(expression, reason), cause);
		this.expression = expression;
	}

	public String getExpression() {
		return expression;
	}

}
