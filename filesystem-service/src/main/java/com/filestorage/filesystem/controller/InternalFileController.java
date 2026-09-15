package com.filestorage.filesystem.controller;


import com.filestorage.filesystem.controller.*;
import com.filestorage.filesystem.domain.*;
import com.filestorage.filesystem.dto.*;
import com.filestorage.filesystem.messaging.*;
import com.filestorage.filesystem.repository.*;
import com.filestorage.filesystem.service.*;
import com.filestorage.common.Headers;
import com.filestorage.common.InternalAuth;
import com.filestorage.common.dto.AccessCheckResponse;
import com.filestorage.common.dto.ActivateFileRequest;
import com.filestorage.common.dto.FolderArchiveResponse;
import com.filestorage.common.dto.CreateInternalFileRequest;
import com.filestorage.common.dto.InternalFileResponse;
import com.filestorage.common.dto.RejectFileRequest;
import com.filestorage.common.dto.ReleaseQuotaRequest;
import com.filestorage.common.dto.UpdateSizeRequest;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/internal")
public class InternalFileController {
    private final FilesystemService service;
    private final String internalToken;

    public InternalFileController(FilesystemService service,
                           @Value("${app.internal-token:dev-internal-token}") String internalToken) {
        this.service = service;
        this.internalToken = internalToken;
    }

    @PostMapping("/files")
    public InternalFileResponse createFile(@RequestHeader(Headers.INTERNAL_TOKEN) String token,
                                    @Valid @RequestBody CreateInternalFileRequest request) {
        InternalAuth.requireToken(token, internalToken);
        return service.createPendingFile(request);
    }

    @PatchMapping("/files/{id}/size")
    public InternalFileResponse updateSize(@RequestHeader(Headers.INTERNAL_TOKEN) String token,
                                    @PathVariable UUID id,
                                    @RequestBody UpdateSizeRequest request) {
        InternalAuth.requireToken(token, internalToken);
        return service.updateSize(id, request.size());
    }

    @PostMapping("/files/{id}/activate")
    public InternalFileResponse activate(@RequestHeader(Headers.INTERNAL_TOKEN) String token,
                                  @PathVariable UUID id,
                                  @Valid @RequestBody ActivateFileRequest request) {
        InternalAuth.requireToken(token, internalToken);
        return service.activate(id, request);
    }

    @PostMapping("/files/{id}/reject")
    public void reject(@RequestHeader(Headers.INTERNAL_TOKEN) String token,
                @PathVariable UUID id,
                @Valid @RequestBody RejectFileRequest request) {
        InternalAuth.requireToken(token, internalToken);
        service.reject(id, request);
    }

    @PostMapping("/quota/release")
    public void releaseQuota(@RequestHeader(Headers.INTERNAL_TOKEN) String token,
                      @RequestBody ReleaseQuotaRequest request) {
        InternalAuth.requireToken(token, internalToken);
        service.reject(request.fileId(), new RejectFileRequest("UPLOAD_ABORTED", null));
    }

    @GetMapping("/files/{id}/access-check")
    public AccessCheckResponse accessCheck(@RequestHeader(Headers.INTERNAL_TOKEN) String token,
                                    @PathVariable UUID id,
                                    @RequestParam UUID userId) {
        InternalAuth.requireToken(token, internalToken);
        return service.accessCheck(id, userId);
    }

    @GetMapping("/folders/{id}/archive")
    public FolderArchiveResponse archiveManifest(@RequestHeader(Headers.INTERNAL_TOKEN) String token,
                                          @PathVariable UUID id,
                                          @RequestParam UUID userId) {
        InternalAuth.requireToken(token, internalToken);
        return service.archiveManifest(userId, id);
    }
}
