package com.chat.repository;

import com.chat.entity.FileRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface FileRecordRepository extends JpaRepository<FileRecord, String> {

    Optional<FileRecord> findByContentHash(String contentHash);

    Optional<FileRecord> findByFileUrl(String fileUrl);
}
