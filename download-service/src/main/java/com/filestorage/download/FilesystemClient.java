package com.filestorage.download;

import com.filestorage.common.Headers;
import com.filestorage.common.dto.AccessCheckResponse;
import com.filestorage.common.dto.FolderArchiveResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.UUID;

@Component
class FilesystemClient {
    private final RestClient restClient;
    private final String internalToken;

    FilesystemClient(RestClient.Builder builder,
                     @Value("${app.filesystem-url:http://localhost:8082}") String filesystemUrl,
                     @Value("${app.internal-token:dev-internal-token}") String internalToken) {
        this.restClient = builder.baseUrl(filesystemUrl).build();
        this.internalToken = internalToken;
    }

    AccessCheckResponse accessCheck(UUID fileId, UUID userId) {
        return restClient.get()
                .uri(uri -> uri.path("/internal/files/{id}/access-check").queryParam("userId", userId).build(fileId))
                .header(Headers.INTERNAL_TOKEN, internalToken)
                .retrieve()
                .body(AccessCheckResponse.class);
    }

    FolderArchiveResponse archiveManifest(UUID folderId, UUID userId) {
        return restClient.get()
                .uri(uri -> uri.path("/internal/folders/{id}/archive").queryParam("userId", userId).build(folderId))
                .header(Headers.INTERNAL_TOKEN, internalToken)
                .retrieve()
                .body(FolderArchiveResponse.class);
    }
}
