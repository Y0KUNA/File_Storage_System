package com.filestorage.virusscan.storage;



import com.filestorage.virusscan.dto.*;
import com.filestorage.virusscan.client.*;
import com.filestorage.virusscan.config.*;
import com.filestorage.virusscan.controller.*;
import com.filestorage.virusscan.messaging.*;
import com.filestorage.virusscan.service.*;
import com.filestorage.virusscan.storage.*;
import io.minio.GetObjectArgs;
import io.minio.MinioClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.InputStream;

@Component
public class ObjectStorageReader {
    private final MinioClient minio;
    private final String bucket;

    public ObjectStorageReader(MinioClient minio,
                        @Value("${app.minio.bucket:file-storage}") String bucket) {
        this.minio = minio;
        this.bucket = bucket;
    }

   public  InputStream open(String storageKey) throws Exception {
        return minio.getObject(GetObjectArgs.builder()
                .bucket(bucket)
                .object(storageKey)
                .build());
    }
}
