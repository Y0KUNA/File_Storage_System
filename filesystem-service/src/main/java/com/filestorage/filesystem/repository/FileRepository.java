package com.filestorage.filesystem.repository;


import com.filestorage.filesystem.dto.*;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;


import com.filestorage.filesystem.domain.*;

public interface FileRepository extends JpaRepository<StoredFile, UUID> {
    public List<StoredFile> findByOwnerIdAndParentFolderId(UUID ownerId, UUID parentFolderId);

   public  boolean existsByOwnerIdAndParentFolderIdAndName(UUID ownerId, UUID parentFolderId, String name);
}
