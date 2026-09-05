package com.filestorage.virusscan;

import com.filestorage.common.Headers;
import com.filestorage.common.dto.ActivateFileRequest;
import com.filestorage.common.dto.RejectFileRequest;
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

    void activate(UUID fileId, String checksum) {
        restClient.post()
                .uri("/internal/files/{id}/activate", fileId)
                .header(Headers.INTERNAL_TOKEN, internalToken)
                .body(new ActivateFileRequest(checksum))
                .retrieve()
                .toBodilessEntity();
    }

    void reject(UUID fileId, String signature) {
        restClient.post()
                .uri("/internal/files/{id}/reject", fileId)
                .header(Headers.INTERNAL_TOKEN, internalToken)
                .body(new RejectFileRequest("INFECTED", signature))
                .retrieve()
                .toBodilessEntity();
    }
}

