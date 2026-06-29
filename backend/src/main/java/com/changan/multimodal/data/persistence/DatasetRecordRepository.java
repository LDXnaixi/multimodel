package com.changan.multimodal.data.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface DatasetRecordRepository extends JpaRepository<DatasetRecord, String> {
    List<DatasetRecord> findAllByOrderByUpdatedAtDesc();
}
