package com.filestorage.upload;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

@Component
class DevObjectStorageGateway implements ObjectStorageGateway {
    private final String publicBaseUrl;

    DevObjectStorageGateway(@Value("${app.dev-storage-url:http://localhost:9000/file-storage}") String publicBaseUrl) {
        this.publicBaseUrl = publicBaseUrl;
    }

    @Override
    public PresignedPut presignPut(String storageKey) {
        String url = UriComponentsBuilder.fromUriString(publicBaseUrl)
                .pathSegment(storageKey)
                .queryParam("devPresigned", "put")
                .toUriString();
        return new PresignedPut(url, Instant.now().plus(5, ChronoUnit.MINUTES));
    }

    @Override
    public long statObjectSize(String storageKey) {
        return 1L;
    }
}

