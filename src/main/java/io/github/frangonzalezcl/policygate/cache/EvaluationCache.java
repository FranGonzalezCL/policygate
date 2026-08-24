package io.github.frangonzalezcl.policygate.cache;

import io.github.frangonzalezcl.policygate.config.CacheProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.Optional;

@Component
public class EvaluationCache {

	private static final Logger log = LoggerFactory.getLogger(EvaluationCache.class);

	private final StringRedisTemplate redisTemplate;
	private final Duration ttl;

	public EvaluationCache(StringRedisTemplate redisTemplate, CacheProperties cacheProperties) {
		this.redisTemplate = redisTemplate;
		this.ttl = cacheProperties.ttl();
	}

	public Optional<Boolean> get(String key) {
		try {
			String value = redisTemplate.opsForValue().get(key);
			if (value == null) {
				log.info("Cache miss for key '{}'", key);
				return Optional.empty();
			}
			log.info("Cache hit for key '{}'", key);
			return Optional.of(Boolean.parseBoolean(value));
		} catch (RuntimeException e) {
			log.warn("Redis read failed for key '{}', evaluating without cache: {}", key, e.getMessage());
			return Optional.empty();
		}
	}

	public void put(String key, boolean value) {
		try {
			redisTemplate.opsForValue().set(key, String.valueOf(value), ttl);
		} catch (RuntimeException e) {
			log.warn("Redis write failed for key '{}': {}", key, e.getMessage());
		}
	}

}
