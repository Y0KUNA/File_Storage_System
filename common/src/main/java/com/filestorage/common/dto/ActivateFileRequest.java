package com.filestorage.common.dto;

import jakarta.validation.constraints.NotBlank;

public record ActivateFileRequest(@NotBlank String checksum) {
}

