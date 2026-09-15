package com.filestorage.upload.dto;


import com.filestorage.upload.client.*;
import com.filestorage.upload.controller.*;
import com.filestorage.upload.domain.*;
import com.filestorage.upload.dto.*;
import com.filestorage.upload.job.*;
import com.filestorage.upload.messaging.*;
import com.filestorage.upload.repository.*;
import com.filestorage.upload.service.*;
import com.filestorage.upload.storage.*;
import com.filestorage.upload.web.*;
import com.filestorage.common.UploadMode;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.Instant;
import java.util.List;
import java.util.UUID;


public record CreateUploadResponse(
        UUID fileId,
        UploadMode mode,
        String uploadUrl,
        UUID uploadSessionId,
        long partSize,
        int totalParts,
        List<PartUploadUrl> parts,
        Instant expiresAt
) {
}
