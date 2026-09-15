package com.filestorage.download.controller;


import com.filestorage.download.client.*;
import com.filestorage.download.controller.*;
import com.filestorage.download.domain.*;
import com.filestorage.download.dto.*;
import com.filestorage.download.messaging.*;
import com.filestorage.download.repository.*;
import com.filestorage.download.service.*;
import com.filestorage.download.storage.*;
import com.filestorage.download.web.*;
import com.filestorage.common.Headers;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
public class DownloadController {
    private final DownloadService service;

    public DownloadController(DownloadService service) {
        this.service = service;
    }

    @GetMapping("/downloads/files/{id}/url")
    public DownloadUrlResponse fileUrl(@RequestHeader(Headers.USER_ID) UUID userId, @PathVariable UUID id) {
        System.out.println(">>> CONTROLLER HIT");
        System.out.println(">>> userId = " + userId);
        System.out.println(">>> fileId = " + id);
        return service.fileUrl(userId, id);
    }

    @PostMapping("/downloads/folders/{id}/zip-jobs")
    @ResponseStatus(HttpStatus.ACCEPTED)
    public ZipJobResponse requestZip(@RequestHeader(Headers.USER_ID) UUID userId, @PathVariable UUID id) {
        
        return service.requestZip(userId, id);
    }

    @GetMapping("/zip-jobs/{id}")
    public ZipJobResponse getZipJob(@PathVariable UUID id) {
        return service.getZipJob(id);
    }

    @PostMapping("/zip-jobs/{id}/download-url")
    public DownloadUrlResponse zipDownloadUrl(@PathVariable UUID id) {
        return service.zipDownloadUrl(id);
    }
}
