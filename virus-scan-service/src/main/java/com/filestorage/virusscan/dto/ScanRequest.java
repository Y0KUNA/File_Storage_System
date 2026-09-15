package com.filestorage.virusscan.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.UUID;

public record ScanRequest(@NotNull UUID fileId, @NotBlank String storageKey) {
}
