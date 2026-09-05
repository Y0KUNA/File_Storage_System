package com.filestorage.virusscan;

import org.springframework.stereotype.Service;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

@Service
class VirusScanService {
    private final FilesystemClient filesystem;
    private final ObjectStorageReader objectStorage;
    private final ClamAvClient clamAv;

    VirusScanService(FilesystemClient filesystem, ObjectStorageReader objectStorage, ClamAvClient clamAv) {
        this.filesystem = filesystem;
        this.objectStorage = objectStorage;
        this.clamAv = clamAv;
    }

    ScanResponse scan(ScanRequest request) {
        try {
            String checksum;
            try (InputStream checksumInput = objectStorage.open(request.storageKey())) {
                checksum = sha256(checksumInput);
            }
            ScanVerdict verdict;
            try (InputStream scanInput = objectStorage.open(request.storageKey())) {
                verdict = clamAv.scan(scanInput);
            }
            if (verdict.clean()) {
                filesystem.activate(request.fileId(), checksum);
                return new ScanResponse(request.fileId(), "CLEAN", checksum);
            }
            filesystem.reject(request.fileId(), verdict.signature());
            return new ScanResponse(request.fileId(), "INFECTED", checksum);
        } catch (Exception ex) {
            throw new IllegalStateException("Virus scan failed for file " + request.fileId(), ex);
        }
    }

    private String sha256(InputStream input) {
        try {
            MessageDigest messageDigest = MessageDigest.getInstance("SHA-256");
            byte[] buffer = new byte[8192];
            int read;
            while ((read = input.read(buffer)) != -1) {
                messageDigest.update(buffer, 0, read);
            }
            byte[] digest = messageDigest.digest();
            StringBuilder hex = new StringBuilder();
            for (byte b : digest) {
                hex.append(String.format("%02x", b));
            }
            return hex.toString();
        } catch (Exception ex) {
            throw new IllegalStateException(ex);
        }
    }
}
