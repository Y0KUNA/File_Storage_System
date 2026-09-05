package com.filestorage.virusscan;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/scan")
class VirusScanController {
    private final VirusScanService service;

    VirusScanController(VirusScanService service) {
        this.service = service;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.ACCEPTED)
    ScanResponse scan(@Valid @RequestBody ScanRequest request) {
        return service.scan(request);
    }
}

