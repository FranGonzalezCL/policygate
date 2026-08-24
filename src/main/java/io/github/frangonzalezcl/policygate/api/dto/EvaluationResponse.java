package io.github.frangonzalezcl.policygate.api.dto;

public record EvaluationResponse(boolean result, String name, int version, boolean cached) {
}
