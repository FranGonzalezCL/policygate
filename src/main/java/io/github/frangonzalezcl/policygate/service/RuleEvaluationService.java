package io.github.frangonzalezcl.policygate.service;

import io.github.frangonzalezcl.policygate.cache.CacheKey;
import io.github.frangonzalezcl.policygate.cache.ContextCanonicalizer;
import io.github.frangonzalezcl.policygate.cache.EvaluationCache;
import io.github.frangonzalezcl.policygate.domain.Rule;
import io.github.frangonzalezcl.policygate.evaluation.ExpressionSyntaxValidator;
import io.github.frangonzalezcl.policygate.evaluation.RestrictedSpelEvaluator;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.Optional;

@Service
public class RuleEvaluationService {

	public record EvaluationOutcome(String name, int version, boolean result, boolean cached) {
	}

	private final RulePublicationService publicationService;
	private final ExpressionSyntaxValidator syntaxValidator;
	private final RestrictedSpelEvaluator evaluator;
	private final ContextCanonicalizer canonicalizer;
	private final EvaluationCache cache;

	public RuleEvaluationService(RulePublicationService publicationService,
			ExpressionSyntaxValidator syntaxValidator,
			RestrictedSpelEvaluator evaluator,
			ContextCanonicalizer canonicalizer,
			EvaluationCache cache) {
		this.publicationService = publicationService;
		this.syntaxValidator = syntaxValidator;
		this.evaluator = evaluator;
		this.canonicalizer = canonicalizer;
		this.cache = cache;
	}

	public EvaluationOutcome evaluate(String name, Map<String, Object> context) {
		Rule rule = publicationService.findActive(name);

		String key = CacheKey.of(rule.getName(), rule.getVersion(), canonicalizer.hash(context));

		Optional<Boolean> cached = cache.get(key);
		if (cached.isPresent()) {
			return new EvaluationOutcome(rule.getName(), rule.getVersion(), cached.get(), true);
		}

		boolean result = evaluator.evaluate(syntaxValidator.parse(rule.getExpression()), context);

		cache.put(key, result);

		return new EvaluationOutcome(rule.getName(), rule.getVersion(), result, false);
	}

}
