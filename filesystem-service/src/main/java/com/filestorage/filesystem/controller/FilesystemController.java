package com.filestorage.filesystem.controller;


import com.filestorage.filesystem.controller.*;
import com.filestorage.filesystem.domain.*;
import com.filestorage.filesystem.dto.*;
import com.filestorage.filesystem.messaging.*;
import com.filestorage.filesystem.repository.*;
import com.filestorage.filesystem.service.*;
import com.filestorage.common.Headers;
import com.filestorage.common.ResourceType;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
public class FilesystemController {
    private final FilesystemService service;

    public FilesystemController(FilesystemService service) {
        this.service = service;
    }

    @PostMapping("/internal/users/{userId}/provision")
    public FolderResponse provisionRoot(@PathVariable UUID userId) {
        return service.provisionRoot(userId);
    }

    @GetMapping("/folders/root")
    public FolderResponse root(@RequestHeader(Headers.USER_ID) UUID userId) {
        return service.root(userId);
    }

    @GetMapping("/folders/{id}/children")
    public ChildrenResponse children(@RequestHeader(Headers.USER_ID) UUID userId, @PathVariable UUID id) {
        return service.children(userId, id);
    }

    @PostMapping("/folders")
    @ResponseStatus(HttpStatus.CREATED)
    public FolderResponse createFolder(@RequestHeader(Headers.USER_ID) UUID userId,
                                @Valid @RequestBody CreateFolderRequest request) {
        return service.createFolder(userId, request);
    }

    @GetMapping("/quota")
    public QuotaResponse quota(@RequestHeader(Headers.USER_ID) UUID userId) {
        return service.quota(userId);
    }

    @DeleteMapping("/resources/{type}/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void moveToTrash(@RequestHeader(Headers.USER_ID) UUID userId,
                     @PathVariable ResourceType type,
                     @PathVariable UUID id) {
        service.moveToTrash(userId, type, id);
    }

    @GetMapping("/trash")
    public TrashResponse trash(@RequestHeader(Headers.USER_ID) UUID userId) {
        return service.trash(userId);
    }

    @PostMapping("/trash/{type}/{id}/restore")
    public RestoreResponse restore(@RequestHeader(Headers.USER_ID) UUID userId,
                            @PathVariable ResourceType type,
                            @PathVariable UUID id) {
        return service.restore(userId, type, id);
    }

    @DeleteMapping("/trash/{type}/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void purge(@RequestHeader(Headers.USER_ID) UUID userId,
               @PathVariable ResourceType type,
               @PathVariable UUID id,
               @RequestParam(defaultValue = "false") boolean permanent) {
        service.purge(userId, type, id, permanent);
    }
}
