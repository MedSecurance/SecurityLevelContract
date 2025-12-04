package edu.upc.dmag.signinginterface.config;
import org.springframework.beans.factory.annotation.Value;
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

    @Value("${minio.access.name}")
    String accessKey;
    @Value("${minio.access.secret}")
    String secretKey;
    @Value("${minio.url}")
    String minioUrl;
    @Value("${minio.account.id}")
    String accountId;


    @Bean
    public S3AsyncClient s3AsyncClient() {
        var credentials = AwsBasicCredentials.builder().accountId(accountId).accessKeyId(accessKey).secretAccessKey(secretKey).build();
        return S3AsyncClient.builder()
                .endpointOverride(URI.create(minioUrl))
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
