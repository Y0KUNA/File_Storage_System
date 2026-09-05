package com.filestorage.download;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

interface ZipJobRepository extends JpaRepository<ZipJob, UUID> {
}

