package com.filestorage.download.client;


import com.filestorage.download.client.*;
import com.filestorage.download.controller.*;
import com.filestorage.download.domain.*;
import com.filestorage.download.dto.*;
import com.filestorage.download.messaging.*;
import com.filestorage.download.repository.*;
import com.filestorage.download.service.*;
import com.filestorage.download.storage.*;
import com.filestorage.download.web.*;
import com.filestorage.common.Headers;
import com.filestorage.common.dto.AccessCheckResponse;
import com.filestorage.common.dto.FolderArchiveResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.UUID;

@Component
public class FilesystemClient {
    private final RestClient restClient;
    private final String internalToken;

    public FilesystemClient(RestClient.Builder builder,
                     @Value("${app.filesystem-url:http://localhost:8082}") String filesystemUrl,
                     @Value("${app.internal-token:dev-internal-token}") String internalToken) {
        this.restClient = builder.baseUrl(filesystemUrl).build();
        this.internalToken = internalToken;
    }

   public  AccessCheckResponse accessCheck(UUID fileId, UUID userId) {
        return restClient.get()
                .uri(uri -> uri.path("/internal/files/{id}/access-check").queryParam("userId", userId).build(fileId))
                .header(Headers.INTERNAL_TOKEN, internalToken)
                .retrieve()
                .body(AccessCheckResponse.class);
    }

   public  FolderArchiveResponse archiveManifest(UUID folderId, UUID userId) {
        return restClient.get()
                .uri(uri -> uri.path("/internal/folders/{id}/archive").queryParam("userId", userId).build(folderId))
                .header(Headers.INTERNAL_TOKEN, internalToken)
                .retrieve()
                .body(FolderArchiveResponse.class);
    }
}
