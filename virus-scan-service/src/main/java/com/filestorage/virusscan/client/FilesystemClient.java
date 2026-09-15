package com.filestorage.virusscan.client;



import com.filestorage.virusscan.dto.*;
import com.filestorage.virusscan.client.*;
import com.filestorage.virusscan.config.*;
import com.filestorage.virusscan.controller.*;
import com.filestorage.virusscan.messaging.*;
import com.filestorage.virusscan.service.*;
import com.filestorage.virusscan.storage.*;
import com.filestorage.common.Headers;
import com.filestorage.common.dto.ActivateFileRequest;
import com.filestorage.common.dto.RejectFileRequest;
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

   public  void activate(UUID fileId, String checksum) {
        restClient.post()
                .uri("/internal/files/{id}/activate", fileId)
                .header(Headers.INTERNAL_TOKEN, internalToken)
                .body(new ActivateFileRequest(checksum))
                .retrieve()
                .toBodilessEntity();
    }

   public  void reject(UUID fileId, String signature) {
        restClient.post()
                .uri("/internal/files/{id}/reject", fileId)
                .header(Headers.INTERNAL_TOKEN, internalToken)
                .body(new RejectFileRequest("INFECTED", signature))
                .retrieve()
                .toBodilessEntity();
    }
}

