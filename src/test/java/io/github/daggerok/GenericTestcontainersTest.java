package io.github.daggerok;

import java.time.Duration;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;
import redis.clients.jedis.Jedis;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * See <a href="https://java.testcontainers.org/quickstart/junit_5_quickstart/">JUnit 5 Quickstart page on Testcontainers site</a>
 */
@Slf4j
@Testcontainers
@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class GenericTestcontainersTest {

  @Container
  @SuppressWarnings("rawtypes")
  GenericContainer<?> redis = new GenericContainer(DockerImageName.parse("redis:6-alpine"))
    .waitingFor(Wait.forListeningPort().withStartupTimeout(Duration.ofSeconds(30)))
    .withExposedPorts(6379)
    .withReuse(true);

  @Test
  void should_test_redis() {
    // given
    var redisAddress = this.redis.getHost();
    var redisPort = this.redis.getMappedPort(6379);

    // when
    try (var redisClient = new Jedis(redisAddress, redisPort)) {
      var setResult = redisClient.set("hello", "world");
      log.info("set result: {}", setResult);
      assertThat(setResult).isEqualTo("OK");

      // then
      var getResult = redisClient.get("hello");
      log.info("get result: {}", getResult);
      assertThat(getResult).isEqualTo("world");
    }
  }
}
