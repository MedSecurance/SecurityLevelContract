package edu.upc.dmag.signinginterface.config;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3AsyncClient;
import software.amazon.awssdk.services.s3.S3Configuration;

import java.net.URI;

@Configuration
public class MinioConfig {

    @Bean
    public S3AsyncClient s3AsyncClient() {
        var credentials = AwsBasicCredentials.builder().accountId("local").accessKeyId("myuseraccesskey").secretAccessKey("myusersecretkey").build();
        return S3AsyncClient.builder()
                .endpointOverride(URI.create("http://signer.minio:9000"))
                .credentialsProvider(
                        StaticCredentialsProvider.create( credentials )
                )
                .region(Region.US_EAST_1) // MinIO ignores region but required by SDK
                .serviceConfiguration(
                        S3Configuration.builder()
                                .pathStyleAccessEnabled(true) // REQUIRED for MinIO
                                .build()
                )
                .build();
    }
}


