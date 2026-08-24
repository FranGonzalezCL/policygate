package io.github.frangonzalezcl.policygate.domain;

// Single source of truth for these bounds, shared later by DTO validation and the Flyway migration.
public final class RuleConstraints {

	public static final String NAME_PATTERN = "^[a-z0-9][a-z0-9_-]{0,63}$";

	public static final int NAME_MAX_LENGTH = 64;

	public static final int EXPRESSION_MAX_LENGTH = 1000;

	private RuleConstraints() {
	}

}
