package com.filestorage.virusscan.dto;

import java.util.UUID;

public record ScanResponse(UUID fileId, String result, String checksum) {
}
