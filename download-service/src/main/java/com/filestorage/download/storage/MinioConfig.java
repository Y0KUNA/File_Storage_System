package com.filestorage.download.storage;


import com.filestorage.download.client.*;
import com.filestorage.download.controller.*;
import com.filestorage.download.domain.*;
import com.filestorage.download.dto.*;
import com.filestorage.download.messaging.*;
import com.filestorage.download.repository.*;
import com.filestorage.download.service.*;
import com.filestorage.download.storage.*;
import com.filestorage.download.web.*;
import io.minio.MinioClient;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

@Configuration
public class MinioConfig {

    @Bean
    @Primary 
    public MinioClient minioClient(
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
    public MinioClient publicMinioClient(
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
