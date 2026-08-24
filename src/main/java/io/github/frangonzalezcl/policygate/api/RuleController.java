package io.github.frangonzalezcl.policygate.api;

import io.github.frangonzalezcl.policygate.api.dto.EvaluationResponse;
import io.github.frangonzalezcl.policygate.api.dto.PublishRuleRequest;
import io.github.frangonzalezcl.policygate.api.dto.RuleResponse;
import io.github.frangonzalezcl.policygate.domain.Rule;
import io.github.frangonzalezcl.policygate.service.RuleEvaluationService;
import io.github.frangonzalezcl.policygate.service.RulePublicationService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping(path = "/rules", produces = MediaType.APPLICATION_JSON_VALUE)
public class RuleController {

	private final RulePublicationService publicationService;
	private final RuleEvaluationService evaluationService;

	public RuleController(RulePublicationService publicationService, RuleEvaluationService evaluationService) {
		this.publicationService = publicationService;
		this.evaluationService = evaluationService;
	}

	@PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<RuleResponse> publish(@Valid @RequestBody PublishRuleRequest request) {
		Rule published = publicationService.publish(request.name(), request.expression());
		return ResponseEntity.status(HttpStatus.CREATED).body(RuleResponse.from(published));
	}

	@GetMapping("/{name}")
	public RuleResponse findActive(@PathVariable String name) {
		return RuleResponse.from(publicationService.findActive(name));
	}

	@PostMapping(path = "/{name}/evaluate", consumes = MediaType.APPLICATION_JSON_VALUE)
	public EvaluationResponse evaluate(@PathVariable String name, @RequestBody Map<String, Object> context) {
		var outcome = evaluationService.evaluate(name, context);
		return new EvaluationResponse(outcome.result(), outcome.name(), outcome.version(), outcome.cached());
	}

}
