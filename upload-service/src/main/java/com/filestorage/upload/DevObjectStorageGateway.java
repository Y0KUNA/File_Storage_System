package com.filestorage.upload;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.util.UriComponentsBuilder;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

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

    @Override
    public PresignedPut presignPart(String storageKey, int partNumber) {
        return presignPut(storageKey + ".part" + partNumber);
    }

    @Override
    public long completeMultipart(String storageKey, int totalParts) {
        return totalParts;
    }

    @Override
    public void abortMultipart(String storageKey, int totalParts) {
    }
}
