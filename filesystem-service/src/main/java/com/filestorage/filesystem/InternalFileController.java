package com.filestorage.filesystem;

import com.filestorage.common.Headers;
import com.filestorage.common.InternalAuth;
import com.filestorage.common.dto.AccessCheckResponse;
import com.filestorage.common.dto.ActivateFileRequest;
import com.filestorage.common.dto.CreateInternalFileRequest;
import com.filestorage.common.dto.InternalFileResponse;
import com.filestorage.common.dto.RejectFileRequest;
import com.filestorage.common.dto.UpdateSizeRequest;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/internal")
class InternalFileController {
    private final FilesystemService service;
    private final String internalToken;

    InternalFileController(FilesystemService service,
                           @Value("${app.internal-token:dev-internal-token}") String internalToken) {
        this.service = service;
        this.internalToken = internalToken;
    }

    @PostMapping("/files")
    InternalFileResponse createFile(@RequestHeader(Headers.INTERNAL_TOKEN) String token,
                                    @Valid @RequestBody CreateInternalFileRequest request) {
        InternalAuth.requireToken(token, internalToken);
        return service.createPendingFile(request);
    }

    @PatchMapping("/files/{id}/size")
    InternalFileResponse updateSize(@RequestHeader(Headers.INTERNAL_TOKEN) String token,
                                    @PathVariable UUID id,
                                    @RequestBody UpdateSizeRequest request) {
        InternalAuth.requireToken(token, internalToken);
        return service.updateSize(id, request.size());
    }

    @PostMapping("/files/{id}/activate")
    InternalFileResponse activate(@RequestHeader(Headers.INTERNAL_TOKEN) String token,
                                  @PathVariable UUID id,
                                  @Valid @RequestBody ActivateFileRequest request) {
        InternalAuth.requireToken(token, internalToken);
        return service.activate(id, request);
    }

    @PostMapping("/files/{id}/reject")
    void reject(@RequestHeader(Headers.INTERNAL_TOKEN) String token,
                @PathVariable UUID id,
                @Valid @RequestBody RejectFileRequest request) {
        InternalAuth.requireToken(token, internalToken);
        service.reject(id, request);
    }

    @GetMapping("/files/{id}/access-check")
    AccessCheckResponse accessCheck(@RequestHeader(Headers.INTERNAL_TOKEN) String token,
                                    @PathVariable UUID id,
                                    @RequestParam UUID userId) {
        InternalAuth.requireToken(token, internalToken);
        return service.accessCheck(id, userId);
    }
}
