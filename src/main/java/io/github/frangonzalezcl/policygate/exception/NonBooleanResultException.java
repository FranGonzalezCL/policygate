package io.github.frangonzalezcl.policygate.exception;

public class NonBooleanResultException extends RuntimeException {

	private final String expression;
	private final Object result;

	public NonBooleanResultException(String expression, Object result) {
		super("Expression '%s' did not evaluate to a boolean; it produced %s".formatted(
				expression, result == null ? "null" : result.getClass().getSimpleName()));
		this.expression = expression;
		this.result = result;
	}

	public String getExpression() {
		return expression;
	}

	public Object getResult() {
		return result;
	}

}
