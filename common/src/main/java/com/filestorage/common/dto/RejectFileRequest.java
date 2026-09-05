package com.filestorage.common.dto;

import jakarta.validation.constraints.NotBlank;

public record RejectFileRequest(@NotBlank String reason, String signature) {
}

