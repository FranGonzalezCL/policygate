package io.github.frangonzalezcl.policygate.exception;

public class VersionConflictException extends RuntimeException {

	private final String name;

	public VersionConflictException(String name, Throwable cause) {
		super("Concurrent publication detected for rule '%s'".formatted(name), cause);
		this.name = name;
	}

	public String getName() {
		return name;
	}

}
