package com.filestorage.upload.client;


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
import com.filestorage.common.Headers;
import com.filestorage.common.dto.CreateInternalFileRequest;
import com.filestorage.common.dto.InternalFileResponse;
import com.filestorage.common.dto.ReleaseQuotaRequest;
import com.filestorage.common.dto.UpdateSizeRequest;
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

   public  InternalFileResponse createPendingFile(CreateInternalFileRequest request) {
        return restClient.post()
                .uri("/internal/files")
                .header(Headers.INTERNAL_TOKEN, internalToken)
                .body(request)
                .retrieve()
                .body(InternalFileResponse.class);
    }

   public  InternalFileResponse updateSize(UUID fileId, long size) {
        return restClient.patch()
                .uri("/internal/files/{id}/size", fileId)
                .header(Headers.INTERNAL_TOKEN, internalToken)
                .body(new UpdateSizeRequest(size))
                .retrieve()
                .body(InternalFileResponse.class);
    }

   public  void releaseQuota(UUID fileId) {
        restClient.post()
                .uri("/internal/quota/release")
                .header(Headers.INTERNAL_TOKEN, internalToken)
                .body(new ReleaseQuotaRequest(fileId))
                .retrieve()
                .toBodilessEntity();
    }
}
