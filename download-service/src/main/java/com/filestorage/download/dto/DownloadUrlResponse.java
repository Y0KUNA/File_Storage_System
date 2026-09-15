package com.filestorage.download.dto;


import com.filestorage.download.client.*;
import com.filestorage.download.controller.*;
import com.filestorage.download.domain.*;
import com.filestorage.download.dto.*;
import com.filestorage.download.messaging.*;
import com.filestorage.download.repository.*;
import com.filestorage.download.service.*;
import com.filestorage.download.storage.*;
import com.filestorage.download.web.*;
import java.time.Instant;
import java.util.UUID;


public record DownloadUrlResponse(String url, Instant expiresAt, boolean supportsRange) {
}
