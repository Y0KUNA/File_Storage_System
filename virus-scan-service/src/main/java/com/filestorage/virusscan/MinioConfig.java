package com.filestorage.virusscan;

import io.minio.MinioClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
class MinioConfig {
    @Bean
    MinioClient minioClient(@Value("${app.minio.endpoint:http://localhost:9000}") String endpoint,
                            @Value("${app.minio.access-key:minio}") String accessKey,
                            @Value("${app.minio.secret-key:minio123}") String secretKey) {
        return MinioClient.builder()
                .endpoint(endpoint)
                .credentials(accessKey, secretKey)
                .build();
    }
}
