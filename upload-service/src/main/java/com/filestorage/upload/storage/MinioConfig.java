package com.filestorage.upload.storage;


import com.filestorage.upload.client.*;
import com.filestorage.upload.controller.*;
import com.filestorage.upload.domain.*;
import com.filestorage.upload.dto.*;
import com.filestorage.upload.job.*;
import com.filestorage.upload.messaging.*;
import com.filestorage.upload.repository.*;
import com.filestorage.upload.service.*;
import com.filestorage.upload.storage.*;
import com.filestorage.upload.web.*;
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