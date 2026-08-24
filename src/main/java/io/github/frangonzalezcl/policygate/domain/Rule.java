package io.github.frangonzalezcl.policygate.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;

@Entity
@Table(name = "rules")
public class Rule {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(name = "name", nullable = false, length = RuleConstraints.NAME_MAX_LENGTH, updatable = false)
	private String name;

	@Column(name = "version", nullable = false, updatable = false)
	private int version;

	@Column(name = "expression", nullable = false, length = RuleConstraints.EXPRESSION_MAX_LENGTH, updatable = false)
	private String expression;

	@Column(name = "active", nullable = false)
	private boolean active;

	@Column(name = "created_at", nullable = false, updatable = false)
	private Instant createdAt;

	protected Rule() {
	}

	private Rule(String name, int version, String expression, boolean active, Instant createdAt) {
		this.name = name;
		this.version = version;
		this.expression = expression;
		this.active = active;
		this.createdAt = createdAt;
	}

	public static Rule publish(String name, int version, String expression) {
		return new Rule(name, version, expression, true, Instant.now());
	}

	// The only mutator of `active`: hard-codes false, so the true -> false direction is structural, not enforced by convention.
	public void deactivate() {
		this.active = false;
	}

	public Long getId() {
		return id;
	}

	public String getName() {
		return name;
	}

	public int getVersion() {
		return version;
	}

	public String getExpression() {
		return expression;
	}

	public boolean isActive() {
		return active;
	}

	public Instant getCreatedAt() {
		return createdAt;
	}

}
