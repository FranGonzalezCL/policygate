package io.github.frangonzalezcl.policygate.service;

import io.github.frangonzalezcl.policygate.domain.Rule;
import io.github.frangonzalezcl.policygate.evaluation.ExpressionSyntaxValidator;
import io.github.frangonzalezcl.policygate.exception.RuleNotFoundException;
import io.github.frangonzalezcl.policygate.exception.VersionConflictException;
import io.github.frangonzalezcl.policygate.repository.RuleRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class RulePublicationService {

	private static final Logger log = LoggerFactory.getLogger(RulePublicationService.class);

	private final RuleRepository repository;
	private final ExpressionSyntaxValidator syntaxValidator;

	public RulePublicationService(RuleRepository repository, ExpressionSyntaxValidator syntaxValidator) {
		this.repository = repository;
		this.syntaxValidator = syntaxValidator;
	}

	@Transactional
	public Rule publish(String name, String expression) {
		syntaxValidator.parse(expression);

		int nextVersion = repository.findMaxVersion(name).orElse(0) + 1;

		try {
			repository.deactivateAll(name);
			Rule published = repository.saveAndFlush(Rule.publish(name, nextVersion, expression));
			log.info("Published rule '{}' version {}", name, nextVersion);
			return published;
		} catch (DataIntegrityViolationException e) {
			throw new VersionConflictException(name, e);
		}
	}

	@Transactional(readOnly = true)
	public Rule findActive(String name) {
		return repository.findByNameAndActiveTrue(name)
				.orElseThrow(() -> new RuleNotFoundException(name));
	}

}
