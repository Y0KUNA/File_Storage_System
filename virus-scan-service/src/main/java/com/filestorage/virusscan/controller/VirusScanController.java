package com.filestorage.virusscan.controller;



import com.filestorage.virusscan.dto.*;
import com.filestorage.virusscan.client.*;
import com.filestorage.virusscan.config.*;
import com.filestorage.virusscan.controller.*;
import com.filestorage.virusscan.messaging.*;
import com.filestorage.virusscan.service.*;
import com.filestorage.virusscan.storage.*;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/scan")
public class VirusScanController {
    private final VirusScanService service;

    public VirusScanController(VirusScanService service) {
        this.service = service;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.ACCEPTED)
    public ScanResponse scan(@Valid @RequestBody ScanRequest request) {
        return service.scan(request);
    }
}

