package com.filestorage.upload.controller;


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
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/uploads")
public class UploadController {
    private final UploadService service;

    public UploadController(UploadService service) {
        this.service = service;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public CreateUploadResponse create(@RequestHeader(Headers.USER_ID) UUID userId,
                                @Valid @RequestBody CreateUploadRequest request) {
        return service.create(userId, request);
    }

    @PostMapping("/{fileId}/confirm")
    @ResponseStatus(HttpStatus.ACCEPTED)
    public ConfirmUploadResponse confirm(@PathVariable UUID fileId) {
        return service.confirm(fileId);
    }

    @GetMapping("/{uploadSessionId}")
    public UploadStatusResponse status(@PathVariable UUID uploadSessionId) {
        return service.status(uploadSessionId);
    }

    @PostMapping("/{uploadSessionId}/parts/presign")
    public PresignPartsResponse presignParts(@PathVariable UUID uploadSessionId,
                                      @RequestBody PresignPartsRequest request) {
        return service.presignParts(uploadSessionId, request);
    }

    @PostMapping("/{uploadSessionId}/complete")
    @ResponseStatus(HttpStatus.ACCEPTED)
    public ConfirmUploadResponse complete(@PathVariable UUID uploadSessionId,
                                   @RequestBody CompleteMultipartRequest request) {
        return service.complete(uploadSessionId, request);
    }

    @DeleteMapping("/{uploadSessionId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void abort(@PathVariable UUID uploadSessionId) {
        service.abort(uploadSessionId);
    }
}
