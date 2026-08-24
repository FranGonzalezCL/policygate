package io.github.frangonzalezcl.policygate.integration;

import io.github.frangonzalezcl.policygate.domain.Rule;
import io.github.frangonzalezcl.policygate.repository.RuleRepository;
import io.github.frangonzalezcl.policygate.support.AbstractIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

// Level 2: the UNIQUE (name, version) constraint is enforced by the database, not just application code (AC-18).
class SchemaConstraintIT extends AbstractIntegrationTest {

	@Autowired
	private RuleRepository ruleRepository;

	@Test
	void duplicateNameAndVersionIsRejectedByTheDatabase() {
		Rule first = Rule.publish("schema_dup_version", 1, "amount > 1");
		ruleRepository.saveAndFlush(first);
		first.deactivate();
		ruleRepository.saveAndFlush(first);

		Rule second = Rule.publish("schema_dup_version", 1, "amount > 2");

		assertThatThrownBy(() -> ruleRepository.saveAndFlush(second))
				.isInstanceOf(DataIntegrityViolationException.class);
	}

}
