package io.github.daggerok;

import java.io.File;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.ComposeContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.core.sync.ResponseTransformer;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * See <a href="https://java.testcontainers.org/modules/docker_compose/">Docker Compose Module page on Testcontainers site</a>
 */
@Slf4j
@Testcontainers
@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class ComposeTestcontainersTest {

  @Container
  ComposeContainer unusedComposeContainer = new ComposeContainer(
    DockerImageName.parse("docker:29.0.1"),
    new File(".tc/docker-compose.yml"))
    .withExposedService("localstack-1", 4566, Wait.forLogMessage(".*Ready.*", 1))
    // .withCopyFilesInContainer("localstack", "docker-compose.yml", "docker-compose-localstack.yml") // default
    .withRemoveImages(ComposeContainer.RemoveImages.LOCAL)
    .withRemoveVolumes(true);

  @Test
  void should_succeeded() {
    // given
    var s3ClientBuilder = S3Client.builder()
      .endpointOverride(URI.create("http://127.0.0.1:4566"))
      .credentialsProvider(StaticCredentialsProvider.create(AwsBasicCredentials.create("test", "test")))
      .region(Region.of("us-east-1"));

    // when
    try (var s3Client = s3ClientBuilder.build()) {
      var bucketName = "my-new-custom-bucket";

      // then
      var listOfBucketsResponse = s3Client.listBuckets();
      log.info("List of buckets response: {}", listOfBucketsResponse);
      assertThat(listOfBucketsResponse.sdkHttpResponse().statusText()).isPresent().hasValue("OK");

      // and
      var buckets = listOfBucketsResponse.buckets();
      log.info("buckets: {}", buckets);
      assertThat(buckets).hasSize(1);
      assertThat(buckets.getFirst().name()).isEqualTo(bucketName);

      // and
      var putObjectResponse = s3Client.putObject(r -> r.bucket(bucketName).key("test"), RequestBody.fromString("Hello World!"));
      assertThat(putObjectResponse.sdkHttpResponse().statusText()).isPresent().hasValue("OK");
      log.info("Put object response: {}", putObjectResponse);

      // and
      var getObjectResponse = s3Client.getObject(r -> r.bucket(bucketName).key("test"), ResponseTransformer.toBytes());
      log.info("Get object response: {}", getObjectResponse);
      assertThat(getObjectResponse.response().sdkHttpResponse().statusText()).isPresent().hasValue("OK");

      // and
      var result = getObjectResponse.asString(StandardCharsets.UTF_8);
      log.info("result: {}", result);
      assertThat(result).isEqualTo("Hello World!");
    }
  }
}
