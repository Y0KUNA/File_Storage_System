package com.filestorage.virusscan.storage;



import com.filestorage.virusscan.dto.*;
import com.filestorage.virusscan.client.*;
import com.filestorage.virusscan.config.*;
import com.filestorage.virusscan.controller.*;
import com.filestorage.virusscan.messaging.*;
import com.filestorage.virusscan.service.*;
import com.filestorage.virusscan.storage.*;
import io.minio.MinioClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class MinioConfig {
    @Bean
    public MinioClient minioClient(@Value("${app.minio.endpoint:http://localhost:9000}") String endpoint,
                            @Value("${app.minio.access-key:minio}") String accessKey,
                            @Value("${app.minio.secret-key:minio123}") String secretKey) {
        return MinioClient.builder()
                .endpoint(endpoint)
                .credentials(accessKey, secretKey)
                .build();
    }
}
