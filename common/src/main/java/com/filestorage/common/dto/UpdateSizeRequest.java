package com.filestorage.common.dto;

import jakarta.validation.constraints.Min;

public record UpdateSizeRequest(@Min(0) long size) {
}

