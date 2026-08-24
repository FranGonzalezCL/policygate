package io.github.frangonzalezcl.policygate.repository;

import io.github.frangonzalezcl.policygate.domain.Rule;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface RuleRepository extends JpaRepository<Rule, Long> {

	Optional<Rule> findByNameAndActiveTrue(String name);

	@Query("select max(r.version) from Rule r where r.name = :name")
	Optional<Integer> findMaxVersion(@Param("name") String name);

	@Modifying(flushAutomatically = false, clearAutomatically = true)
	@Query("update Rule r set r.active = false where r.name = :name and r.active = true")
	int deactivateAll(@Param("name") String name);

}
