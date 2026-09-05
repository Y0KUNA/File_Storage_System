package com.filestorage.download;

import com.filestorage.common.dto.ArchiveFileItem;
import io.minio.BucketExistsArgs;
import io.minio.GetObjectArgs;
import io.minio.GetPresignedObjectUrlArgs;
import io.minio.MakeBucketArgs;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import io.minio.http.Method;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.io.InputStream;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.concurrent.TimeUnit;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

@Component
class DownloadObjectStorage {
    private final MinioClient minio;
    private final String bucket;
    private final int presignExpiryMinutes;
    private volatile boolean bucketReady;

    DownloadObjectStorage(MinioClient minio,
                          @Value("${app.minio.bucket:file-storage}") String bucket,
                          @Value("${app.minio.presign-expiry-minutes:15}") int presignExpiryMinutes) {
        this.minio = minio;
        this.bucket = bucket;
        this.presignExpiryMinutes = presignExpiryMinutes;
    }

    PresignedGet presignGet(String storageKey) {
        ensureBucket();
        try {
            String url = minio.getPresignedObjectUrl(GetPresignedObjectUrlArgs.builder()
                    .bucket(bucket)
                    .object(storageKey)
                    .method(Method.GET)
                    .expiry(presignExpiryMinutes, TimeUnit.MINUTES)
                    .build());
            return new PresignedGet(url, Instant.now().plus(presignExpiryMinutes, ChronoUnit.MINUTES));
        } catch (Exception ex) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "MINIO_PRESIGN_GET_FAILED", ex);
        }
    }

    void writeZipEntry(ZipOutputStream zip, ArchiveFileItem item) {
        ensureBucket();
        try (InputStream input = minio.getObject(GetObjectArgs.builder()
                .bucket(bucket)
                .object(item.storageKey())
                .build())) {
            zip.putNextEntry(new ZipEntry(item.name()));
            input.transferTo(zip);
            zip.closeEntry();
        } catch (Exception ex) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "MINIO_ZIP_STREAM_FAILED", ex);
        }
    }

    void putObject(String storageKey, InputStream input, long size, String contentType) {
        ensureBucket();
        try {
            minio.putObject(PutObjectArgs.builder()
                    .bucket(bucket)
                    .object(storageKey)
                    .stream(input, size, -1)
                    .contentType(contentType)
                    .build());
        } catch (Exception ex) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "MINIO_PUT_OBJECT_FAILED", ex);
        }
    }

    private void ensureBucket() {
        if (bucketReady) {
            return;
        }
        synchronized (this) {
            if (bucketReady) {
                return;
            }
            try {
                boolean exists = minio.bucketExists(BucketExistsArgs.builder().bucket(bucket).build());
                if (!exists) {
                    minio.makeBucket(MakeBucketArgs.builder().bucket(bucket).build());
                }
                bucketReady = true;
            } catch (Exception ex) {
                throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "MINIO_BUCKET_UNAVAILABLE", ex);
            }
        }
    }
}

record PresignedGet(String url, Instant expiresAt) {
}
