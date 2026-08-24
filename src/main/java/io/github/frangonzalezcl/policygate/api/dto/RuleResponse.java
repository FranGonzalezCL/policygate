package io.github.frangonzalezcl.policygate.api.dto;

import io.github.frangonzalezcl.policygate.domain.Rule;

public record RuleResponse(Long id, String name, int version, String expression, boolean active) {

	public static RuleResponse from(Rule rule) {
		return new RuleResponse(rule.getId(), rule.getName(), rule.getVersion(), rule.getExpression(), rule.isActive());
	}

}
