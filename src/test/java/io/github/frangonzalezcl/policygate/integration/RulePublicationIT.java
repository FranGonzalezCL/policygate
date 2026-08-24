package io.github.frangonzalezcl.policygate.integration;

import io.github.frangonzalezcl.policygate.api.dto.PublishRuleRequest;
import io.github.frangonzalezcl.policygate.api.dto.RuleResponse;
import io.github.frangonzalezcl.policygate.domain.Rule;
import io.github.frangonzalezcl.policygate.repository.RuleRepository;
import io.github.frangonzalezcl.policygate.support.AbstractIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

// Level 2: publication, versioning, history conservation, active uniqueness, retrieval.
class RulePublicationIT extends AbstractIntegrationTest {

	@Autowired
	private RuleRepository ruleRepository;

	private ResponseEntity<RuleResponse> publish(String name, String expression) {
		return restTemplate.postForEntity("/rules", new PublishRuleRequest(name, expression), RuleResponse.class);
	}

	@Test
	void publishingANewRuleAssignsVersionOneAndIsRetrievableByName() {
		ResponseEntity<RuleResponse> publishResponse = publish("pub_new_rule", "amount > 100");

		assertThat(publishResponse.getStatusCode()).isEqualTo(HttpStatus.CREATED);
		assertThat(publishResponse.getBody()).isNotNull();
		assertThat(publishResponse.getBody().version()).isEqualTo(1);

		ResponseEntity<RuleResponse> getResponse = restTemplate.getForEntity("/rules/pub_new_rule", RuleResponse.class);

		assertThat(getResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
		assertThat(getResponse.getBody().version()).isEqualTo(1);
		assertThat(getResponse.getBody().expression()).isEqualTo("amount > 100");
	}

	@Test
	void publishingASecondVersionMakesGetReturnTheNewVersion() {
		publish("pub_second_version", "amount > 100");
		publish("pub_second_version", "amount > 200");

		ResponseEntity<RuleResponse> getResponse = restTemplate.getForEntity("/rules/pub_second_version", RuleResponse.class);

		assertThat(getResponse.getBody().version()).isEqualTo(2);
		assertThat(getResponse.getBody().expression()).isEqualTo("amount > 200");
	}

	@Test
	void previousVersionRowSurvivesWithOriginalExpressionAndInactive() {
		publish("pub_history_kept", "amount > 100");
		publish("pub_history_kept", "amount > 200");

		List<Rule> allVersions = ruleRepository.findAll().stream()
				.filter(rule -> rule.getName().equals("pub_history_kept"))
				.toList();

		Rule v1 = allVersions.stream().filter(rule -> rule.getVersion() == 1).findFirst().orElseThrow();
		assertThat(v1.getExpression()).isEqualTo("amount > 100");
		assertThat(v1.isActive()).isFalse();
	}

	@Test
	void atMostOneActiveRowExistsPerName() {
		publish("pub_single_active", "amount > 100");
		publish("pub_single_active", "amount > 200");
		publish("pub_single_active", "amount > 300");

		long activeCount = ruleRepository.findAll().stream()
				.filter(rule -> rule.getName().equals("pub_single_active"))
				.filter(Rule::isActive)
				.count();

		assertThat(activeCount).isEqualTo(1);
	}

	@Test
	void gettingAnUnknownNameReturnsNotFound() {
		ResponseEntity<Map> getResponse = restTemplate.getForEntity("/rules/pub_does_not_exist", Map.class);

		assertThat(getResponse.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
	}

}
