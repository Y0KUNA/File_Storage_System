package com.filestorage.upload;

import io.minio.MinioClient;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

@Configuration
class MinioConfig {

    @Bean
    @Primary 
    MinioClient minioClient(
            @Value("${app.minio.endpoint:http://localhost:9000}") String endpoint,
            @Value("${app.minio.access-key:minio}") String accessKey,
            @Value("${app.minio.secret-key:minio123}") String secretKey) {

        return MinioClient.builder()
                .endpoint(endpoint)
                .region("us-east-1")
                .credentials(accessKey, secretKey)
                .build();
    }

    @Bean
    @Qualifier("publicMinioClient")
    MinioClient publicMinioClient(
            @Value("${app.minio.public-endpoint:http://localhost:9000}") String publicEndpoint,
            @Value("${app.minio.access-key:minio}") String accessKey,
            @Value("${app.minio.secret-key:minio123}") String secretKey) {

        return MinioClient.builder()
                .endpoint(publicEndpoint)
                .region("us-east-1")
                .credentials(accessKey, secretKey)
                .build();
    }
}