package com.filestorage.filesystem.repository;


import com.filestorage.filesystem.dto.*;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;


import com.filestorage.filesystem.domain.*;

public interface TrashItemRepository extends JpaRepository<TrashItem, UUID> {
    public List<TrashItem> findByOwnerIdOrderByTrashedAtDesc(UUID ownerId);

   public  Optional<TrashItem> findByOwnerIdAndResourceIdAndResourceType(UUID ownerId, UUID resourceId, com.filestorage.common.ResourceType resourceType);

   public  boolean existsByResourceIdAndResourceType(UUID resourceId, com.filestorage.common.ResourceType resourceType);
}
