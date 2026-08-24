package io.github.frangonzalezcl.policygate.exception;

public class RuleNotFoundException extends RuntimeException {

	private final String name;

	public RuleNotFoundException(String name) {
		super("No active rule found for name '%s'".formatted(name));
		this.name = name;
	}

	public String getName() {
		return name;
	}

}
