package io.github.frangonzalezcl.policygate.api;

import io.github.frangonzalezcl.policygate.exception.ExpressionEvaluationException;
import io.github.frangonzalezcl.policygate.exception.InvalidExpressionException;
import io.github.frangonzalezcl.policygate.exception.NonBooleanResultException;
import io.github.frangonzalezcl.policygate.exception.RuleNotFoundException;
import io.github.frangonzalezcl.policygate.exception.VersionConflictException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.net.URI;
import java.util.LinkedHashMap;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

	@ExceptionHandler(MethodArgumentNotValidException.class)
	public ResponseEntity<ProblemDetail> handleValidation(MethodArgumentNotValidException ex) {
		ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, "The request body failed validation.");
		problem.setTitle("Invalid request");
		problem.setType(URI.create("/problems/validation-failed"));

		Map<String, String> fieldErrors = new LinkedHashMap<>();
		for (var error : ex.getBindingResult().getFieldErrors()) {
			fieldErrors.put(error.getField(), error.getDefaultMessage());
		}
		problem.setProperty("fields", fieldErrors);

		return ResponseEntity.status(problem.getStatus()).body(problem);
	}

	@ExceptionHandler(HttpMessageNotReadableException.class)
	public ResponseEntity<ProblemDetail> handleUnreadableBody(HttpMessageNotReadableException ex) {
		ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, "The request body is not readable JSON.");
		problem.setTitle("Malformed request body");
		problem.setType(URI.create("/problems/malformed-request-body"));
		return ResponseEntity.status(problem.getStatus()).body(problem);
	}

	@ExceptionHandler(InvalidExpressionException.class)
	public ResponseEntity<ProblemDetail> handleInvalidExpression(InvalidExpressionException ex) {
		ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, ex.getMessage());
		problem.setTitle("Invalid SpEL expression");
		problem.setType(URI.create("/problems/invalid-expression"));
		return ResponseEntity.status(problem.getStatus()).body(problem);
	}

	@ExceptionHandler(RuleNotFoundException.class)
	public ResponseEntity<ProblemDetail> handleRuleNotFound(RuleNotFoundException ex) {
		ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, ex.getMessage());
		problem.setTitle("Rule not found");
		problem.setType(URI.create("/problems/rule-not-found"));
		return ResponseEntity.status(problem.getStatus()).body(problem);
	}

	@ExceptionHandler(VersionConflictException.class)
	public ResponseEntity<ProblemDetail> handleVersionConflict(VersionConflictException ex) {
		ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT,
				"Concurrent publication detected for rule '%s'. Please retry.".formatted(ex.getName()));
		problem.setTitle("Concurrent publication");
		problem.setType(URI.create("/problems/version-conflict"));
		return ResponseEntity.status(problem.getStatus()).body(problem);
	}

	@ExceptionHandler(ExpressionEvaluationException.class)
	public ResponseEntity<ProblemDetail> handleExpressionEvaluation(ExpressionEvaluationException ex) {
		ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.UNPROCESSABLE_ENTITY, ex.getMessage());
		problem.setTitle("Expression could not be evaluated");
		problem.setType(URI.create("/problems/expression-evaluation-failed"));
		return ResponseEntity.status(problem.getStatus()).body(problem);
	}

	@ExceptionHandler(NonBooleanResultException.class)
	public ResponseEntity<ProblemDetail> handleNonBooleanResult(NonBooleanResultException ex) {
		ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.UNPROCESSABLE_ENTITY, ex.getMessage());
		problem.setTitle("Rule did not produce a boolean result");
		problem.setType(URI.create("/problems/non-boolean-result"));
		return ResponseEntity.status(problem.getStatus()).body(problem);
	}

}
