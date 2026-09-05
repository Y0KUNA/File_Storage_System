package com.filestorage.download;

import com.filestorage.common.Headers;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
class DownloadController {
    private final DownloadService service;

    DownloadController(DownloadService service) {
        this.service = service;
    }

    @PostMapping("/downloads/files/{id}/url")
    DownloadUrlResponse fileUrl(@RequestHeader(Headers.USER_ID) UUID userId, @PathVariable UUID id) {
        return service.fileUrl(userId, id);
    }

    @PostMapping("/downloads/folders/{id}/zip-jobs")
    @ResponseStatus(HttpStatus.ACCEPTED)
    ZipJobResponse requestZip(@RequestHeader(Headers.USER_ID) UUID userId, @PathVariable UUID id) {
        return service.requestZip(userId, id);
    }

    @GetMapping("/zip-jobs/{id}")
    ZipJobResponse getZipJob(@PathVariable UUID id) {
        return service.getZipJob(id);
    }

    @PostMapping("/zip-jobs/{id}/download-url")
    DownloadUrlResponse zipDownloadUrl(@PathVariable UUID id) {
        return service.zipDownloadUrl(id);
    }
}

