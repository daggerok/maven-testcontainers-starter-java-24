package io.github.daggerok;

import java.nio.charset.StandardCharsets;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.localstack.LocalStackContainer;
import org.testcontainers.utility.DockerImageName;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.core.sync.ResponseTransformer;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * See <a href="https://java.testcontainers.org/modules/localstack/">LocalStack Module page on Testcontainers site</a>
 */
@Slf4j
@Testcontainers
@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class LocalStackTestcontainersTest {

  @Container
  LocalStackContainer localstack = new LocalStackContainer(DockerImageName.parse("localstack/localstack:4.11.1"))
    .waitingFor(Wait.forHealthcheck())
    .withExposedPorts(4566)
    .withAccessToHost(true)
    .withServices("s3")
    .withReuse(true);

  @Test
  void should_succeeded() {
    // setup
    log.info(localstack.getEndpoint().toString()); // http://127.0.0.1:4566 <- port is going to be mapped (different)
    log.info(localstack.getAccessKey()); // test
    log.info(localstack.getSecretKey()); // test
    log.info(localstack.getRegion()); // us-east-1

    // given
    var s3ClientBuilder = S3Client
      .builder()
      .endpointOverride(localstack.getEndpoint())
      .credentialsProvider(StaticCredentialsProvider.create(AwsBasicCredentials.create(localstack.getAccessKey(), localstack.getSecretKey())))
      .region(Region.of(localstack.getRegion()));

    // when
    try (var s3Client = s3ClientBuilder.build()) {
      var bucketName = "my-new-custom-bucket";
      var createBucketResponse = s3Client.createBucket(r -> r.bucket(bucketName));
      log.info("Create bucket response: {}", createBucketResponse);
      assertThat(createBucketResponse.sdkHttpResponse().statusText()).isPresent().hasValue("OK");
      assertThat(createBucketResponse.location()).endsWith(bucketName);

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
