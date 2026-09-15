package com.filestorage.filesystem.repository;


import com.filestorage.filesystem.dto.*;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;


import com.filestorage.filesystem.domain.*;

public interface FolderRepository extends JpaRepository<FolderNode, UUID> {
    public Optional<FolderNode> findByOwnerIdAndParentIdIsNull(UUID ownerId);

   public  List<FolderNode> findByOwnerIdAndParentId(UUID ownerId, UUID parentId);

   public  boolean existsByOwnerIdAndParentIdAndName(UUID ownerId, UUID parentId, String name);
}
