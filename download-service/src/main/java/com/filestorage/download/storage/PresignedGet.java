package com.filestorage.download.storage;


import com.filestorage.download.client.*;
import com.filestorage.download.controller.*;
import com.filestorage.download.domain.*;
import com.filestorage.download.dto.*;
import com.filestorage.download.messaging.*;
import com.filestorage.download.repository.*;
import com.filestorage.download.service.*;
import com.filestorage.download.storage.*;
import com.filestorage.download.web.*;
import com.filestorage.common.dto.ArchiveFileItem;
import io.minio.BucketExistsArgs;
import io.minio.GetObjectArgs;
import io.minio.GetPresignedObjectUrlArgs;
import io.minio.MakeBucketArgs;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import io.minio.http.Method;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import java.io.InputStream;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

public record PresignedGet(String url, Instant expiresAt) {
}
