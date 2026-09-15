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
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.util.UriComponentsBuilder;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

public class DevObjectStorageGateway implements ObjectStorageGateway {
    private final String publicBaseUrl;

    public DevObjectStorageGateway(@Value("${app.dev-storage-url:http://localhost:9000/file-storage}") String publicBaseUrl) {
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
