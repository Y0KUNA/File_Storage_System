package com.filestorage.upload.storage;


import com.filestorage.upload.client.*;
import com.filestorage.upload.controller.*;
import com.filestorage.upload.domain.*;
import com.filestorage.upload.dto.*;
import com.filestorage.upload.job.*;
import com.filestorage.upload.messaging.*;
import com.filestorage.upload.repository.*;
import com.filestorage.upload.service.*;
import com.filestorage.upload.storage.*;
import com.filestorage.upload.web.*;
import java.time.Instant;

public interface ObjectStorageGateway {
    public PresignedPut presignPut(String storageKey);

   public  PresignedPut presignPart(String storageKey, int partNumber);

   public  long statObjectSize(String storageKey);

   public  long completeMultipart(String storageKey, int totalParts);

   public  void abortMultipart(String storageKey, int totalParts);
}
