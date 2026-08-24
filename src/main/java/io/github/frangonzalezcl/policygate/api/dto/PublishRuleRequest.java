package io.github.frangonzalezcl.policygate.api.dto;

import io.github.frangonzalezcl.policygate.domain.RuleConstraints;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record PublishRuleRequest(

		@NotBlank
		@Pattern(regexp = RuleConstraints.NAME_PATTERN)
		@Size(max = RuleConstraints.NAME_MAX_LENGTH)
		String name,

		@NotBlank
		@Size(max = RuleConstraints.EXPRESSION_MAX_LENGTH)
		String expression

) {
}
