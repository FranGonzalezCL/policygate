package io.github.frangonzalezcl.policygate.support;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.resttestclient.TestRestTemplate;
import org.springframework.boot.resttestclient.autoconfigure.AutoConfigureTestRestTemplate;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;

// Singleton container pattern: no @Testcontainers/@Container, which stop the containers
// after every test class even when the fields are static. Starting them once in a static
// initializer on this shared base is what actually makes them live for the whole suite
// (SPEC-05-R17, [D-17]); the JVM shutdown hook Testcontainers registers reaps them at exit.
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureTestRestTemplate
public abstract class AbstractIntegrationTest {

	@ServiceConnection
	static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>(DockerImageName.parse("postgres:16-alpine"));

	@ServiceConnection("redis")
	static final GenericContainer<?> REDIS = new GenericContainer<>(DockerImageName.parse("redis:7-alpine"))
			.withExposedPorts(6379);

	static {
		POSTGRES.start();
		REDIS.start();
	}

	@Autowired
	protected TestRestTemplate restTemplate;

	@Autowired
	protected StringRedisTemplate redisTemplate;

}
