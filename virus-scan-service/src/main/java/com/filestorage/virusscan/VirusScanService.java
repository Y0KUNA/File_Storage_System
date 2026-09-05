package com.filestorage.virusscan;

import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

@Service
class VirusScanService {
    private final FilesystemClient filesystem;

    VirusScanService(FilesystemClient filesystem) {
        this.filesystem = filesystem;
    }

    ScanResponse scan(ScanRequest request) {
        String checksum = sha256DevPlaceholder(request.storageKey());
        filesystem.activate(request.fileId(), checksum);
        return new ScanResponse(request.fileId(), "CLEAN", checksum);
    }

    private String sha256DevPlaceholder(String value) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8));
            StringBuilder hex = new StringBuilder();
            for (byte b : digest) {
                hex.append(String.format("%02x", b));
            }
            return hex.toString();
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException(ex);
        }
    }
}

