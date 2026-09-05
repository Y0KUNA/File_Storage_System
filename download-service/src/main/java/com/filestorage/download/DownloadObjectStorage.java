package com.filestorage.download;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

@Component
class DownloadObjectStorage {
    private final String publicBaseUrl;

    DownloadObjectStorage(@Value("${app.dev-storage-url:http://localhost:9000/file-storage}") String publicBaseUrl) {
        this.publicBaseUrl = publicBaseUrl;
    }

    PresignedGet presignGet(String storageKey) {
        String url = UriComponentsBuilder.fromUriString(publicBaseUrl)
                .pathSegment(storageKey)
                .queryParam("devPresigned", "get")
                .toUriString();
        return new PresignedGet(url, Instant.now().plus(5, ChronoUnit.MINUTES));
    }
}

record PresignedGet(String url, Instant expiresAt) {
}

