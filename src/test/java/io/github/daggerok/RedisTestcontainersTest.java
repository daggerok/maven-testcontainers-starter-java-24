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

@Slf4j
@Testcontainers
@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class RedisTestcontainersTest {

  @Container
  public GenericContainer redis = new GenericContainer(DockerImageName.parse("redis:6-alpine"))
    .waitingFor(Wait.forListeningPort().withStartupTimeout(Duration.ofSeconds(30)))
    .withExposedPorts(6379);

  @Test
  void should_succeeded() {
    // given
    var redisAddress = this.redis.getContainerIpAddress();
    var redisPort = this.redis.getMappedPort(6379);
    var redisClient = new Jedis(redisAddress, redisPort);

    // when
    var setResult = redisClient.set("hello", "world");
    log.info("set result: {}", setResult);
    assertThat(setResult).isEqualTo("OK");

    // then
    var getResult = redisClient.get("hello");
    log.info("get result: {}", getResult);
    // and
    assertThat(getResult).isEqualTo("world");
  }
}
