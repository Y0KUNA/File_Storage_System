package com.filestorage.filesystem.dto;


import com.filestorage.filesystem.controller.*;
import com.filestorage.filesystem.domain.*;
import com.filestorage.filesystem.dto.*;
import com.filestorage.filesystem.messaging.*;
import com.filestorage.filesystem.repository.*;
import com.filestorage.filesystem.service.*;
import com.filestorage.common.FileStatus;
import com.filestorage.common.ResourceType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.Instant;
import java.util.List;
import java.util.UUID;


public record TrashItemResponse(UUID id, UUID resourceId, ResourceType resourceType, String name, UUID originalParentId, long size, Instant trashedAt) {
}
