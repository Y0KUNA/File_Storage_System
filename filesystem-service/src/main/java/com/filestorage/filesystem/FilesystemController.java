package com.filestorage.filesystem;

import com.filestorage.common.Headers;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
class FilesystemController {
    private final FilesystemService service;

    FilesystemController(FilesystemService service) {
        this.service = service;
    }

    @PostMapping("/internal/users/{userId}/provision")
    FolderResponse provisionRoot(@PathVariable UUID userId) {
        return service.provisionRoot(userId);
    }

    @GetMapping("/folders/root")
    FolderResponse root(@RequestHeader(Headers.USER_ID) UUID userId) {
        return service.root(userId);
    }

    @GetMapping("/folders/{id}/children")
    ChildrenResponse children(@RequestHeader(Headers.USER_ID) UUID userId, @PathVariable UUID id) {
        return service.children(userId, id);
    }

    @PostMapping("/folders")
    @ResponseStatus(HttpStatus.CREATED)
    FolderResponse createFolder(@RequestHeader(Headers.USER_ID) UUID userId,
                                @Valid @RequestBody CreateFolderRequest request) {
        return service.createFolder(userId, request);
    }

    @GetMapping("/quota")
    QuotaResponse quota(@RequestHeader(Headers.USER_ID) UUID userId) {
        return service.quota(userId);
    }
}

