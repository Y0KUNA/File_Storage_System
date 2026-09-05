package com.filestorage.virusscan;

import io.minio.GetObjectArgs;
import io.minio.MinioClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.InputStream;

@Component
class ObjectStorageReader {
    private final MinioClient minio;
    private final String bucket;

    ObjectStorageReader(MinioClient minio,
                        @Value("${app.minio.bucket:file-storage}") String bucket) {
        this.minio = minio;
        this.bucket = bucket;
    }

    InputStream open(String storageKey) throws Exception {
        return minio.getObject(GetObjectArgs.builder()
                .bucket(bucket)
                .object(storageKey)
                .build());
    }
}
