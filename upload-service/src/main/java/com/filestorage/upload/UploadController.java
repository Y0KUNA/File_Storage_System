package com.filestorage.upload;

import com.filestorage.common.Headers;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/uploads")
class UploadController {
    private final UploadService service;

    UploadController(UploadService service) {
        this.service = service;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    CreateUploadResponse create(@RequestHeader(Headers.USER_ID) UUID userId,
                                @Valid @RequestBody CreateUploadRequest request) {
        return service.create(userId, request);
    }

    @PostMapping("/{fileId}/confirm")
    @ResponseStatus(HttpStatus.ACCEPTED)
    ConfirmUploadResponse confirm(@PathVariable UUID fileId) {
        return service.confirm(fileId);
    }

    @GetMapping("/{uploadSessionId}")
    UploadStatusResponse status(@PathVariable UUID uploadSessionId) {
        return service.status(uploadSessionId);
    }

    @PostMapping("/{uploadSessionId}/parts/presign")
    PresignPartsResponse presignParts(@PathVariable UUID uploadSessionId,
                                      @RequestBody PresignPartsRequest request) {
        return service.presignParts(uploadSessionId, request);
    }

    @PostMapping("/{uploadSessionId}/complete")
    @ResponseStatus(HttpStatus.ACCEPTED)
    ConfirmUploadResponse complete(@PathVariable UUID uploadSessionId,
                                   @RequestBody CompleteMultipartRequest request) {
        return service.complete(uploadSessionId, request);
    }

    @DeleteMapping("/{uploadSessionId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void abort(@PathVariable UUID uploadSessionId) {
        service.abort(uploadSessionId);
    }
}
