package com.filestorage.upload;

import io.minio.BucketExistsArgs;
import io.minio.ComposeObjectArgs;
import io.minio.ComposeSource;
import io.minio.GetPresignedObjectUrlArgs;
import io.minio.MakeBucketArgs;
import io.minio.MinioClient;
import io.minio.RemoveObjectArgs;
import io.minio.StatObjectArgs;
import io.minio.http.Method;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Component
class MinioObjectStorageGateway implements ObjectStorageGateway {

    // Dùng cho các thao tác từ upload-service -> MinIO
    // Endpoint: http://minio:9000
    private final MinioClient minio;

    // Dùng để tạo presigned URL cho browser
    // Endpoint: http://localhost:9000
    private final MinioClient publicMinio;

    private final String bucket;
    private final int presignExpiryMinutes;

    private volatile boolean bucketReady;

    MinioObjectStorageGateway(
            MinioClient minio,
            @Qualifier("publicMinioClient") MinioClient publicMinio,
            @Value("${app.minio.bucket:file-storage}") String bucket,
            @Value("${app.minio.presign-expiry-minutes:15}") int presignExpiryMinutes) {

        this.minio = minio;
        this.publicMinio = publicMinio;
        this.bucket = bucket;
        this.presignExpiryMinutes = presignExpiryMinutes;
    }

    @Override
    public PresignedPut presignPut(String storageKey) {
        // Kiểm tra bucket bằng MinIO nội bộ
        ensureBucket();

        try {
            // QUAN TRỌNG:
            // Dùng publicMinio để URL trả về cho browser có hostname localhost
            String url = publicMinio.getPresignedObjectUrl(
                    GetPresignedObjectUrlArgs.builder()
                            .bucket(bucket)
                            .object(storageKey)
                            .method(Method.PUT)
                            .expiry(presignExpiryMinutes, TimeUnit.MINUTES)
                            .build()
            );

            return new PresignedPut(
                    url,
                    Instant.now().plus(
                            presignExpiryMinutes,
                            ChronoUnit.MINUTES
                    )
            );

        } catch (Exception ex) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_GATEWAY,
                    "MINIO_PRESIGN_PUT_FAILED",
                    ex
            );
        }
    }

    @Override
    public PresignedPut presignPart(String storageKey, int partNumber) {
        return presignPut(partKey(storageKey, partNumber));
    }

    @Override
    public long statObjectSize(String storageKey) {
        ensureBucket();

        try {
            return minio.statObject(
                    StatObjectArgs.builder()
                            .bucket(bucket)
                            .object(storageKey)
                            .build()
            ).size();

        } catch (Exception ex) {
            throw new ResponseStatusException(
                    HttpStatus.NOT_FOUND,
                    "OBJECT_NOT_FOUND_IN_STORAGE",
                    ex
            );
        }
    }

    @Override
    public long completeMultipart(String storageKey, int totalParts) {
        ensureBucket();

        try {
            List<ComposeSource> sources = new ArrayList<>();
            long size = 0;

            for (int partNumber = 1; partNumber <= totalParts; partNumber++) {
                String partKey = partKey(storageKey, partNumber);

                size += statObjectSize(partKey);

                sources.add(
                        ComposeSource.builder()
                                .bucket(bucket)
                                .object(partKey)
                                .build()
                );
            }

            minio.composeObject(
                    ComposeObjectArgs.builder()
                            .bucket(bucket)
                            .object(storageKey)
                            .sources(sources)
                            .build()
            );

            abortMultipart(storageKey, totalParts);

            return size;

        } catch (ResponseStatusException ex) {
            throw ex;

        } catch (Exception ex) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_GATEWAY,
                    "MINIO_MULTIPART_COMPLETE_FAILED",
                    ex
            );
        }
    }

    @Override
    public void abortMultipart(String storageKey, int totalParts) {
        ensureBucket();

        for (int partNumber = 1; partNumber <= totalParts; partNumber++) {
            try {
                minio.removeObject(
                        RemoveObjectArgs.builder()
                                .bucket(bucket)
                                .object(partKey(storageKey, partNumber))
                                .build()
                );
            } catch (Exception ignored) {
            }
        }
    }

    private String partKey(String storageKey, int partNumber) {
        return storageKey + ".parts/" + partNumber;
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
                // Luôn dùng minio INTERNAL ở đây
                boolean exists = minio.bucketExists(
                        BucketExistsArgs.builder()
                                .bucket(bucket)
                                .build()
                );

                if (!exists) {
                    minio.makeBucket(
                            MakeBucketArgs.builder()
                                    .bucket(bucket)
                                    .build()
                    );
                }

                bucketReady = true;

            } catch (Exception ex) {
                throw new ResponseStatusException(
                        HttpStatus.BAD_GATEWAY,
                        "MINIO_BUCKET_UNAVAILABLE",
                        ex
                );
            }
        }
    }
}