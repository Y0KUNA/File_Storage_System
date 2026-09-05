package com.filestorage.virusscan;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

record ScanRequest(@NotNull UUID fileId, @NotBlank String storageKey) {
}

record ScanResponse(UUID fileId, String result, String checksum) {
}

