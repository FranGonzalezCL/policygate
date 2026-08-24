package io.github.frangonzalezcl.policygate.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

import java.time.Duration;

@ConfigurationProperties("policygate.cache")
public record CacheProperties(@DefaultValue("10m") Duration ttl) {
}
