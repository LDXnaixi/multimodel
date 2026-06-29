package com.changan.multimodal.data.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SampleRecordRepository extends JpaRepository<SampleRecord, String> {
    List<SampleRecord> findByDatasetIdOrderByCreatedAtDesc(String datasetId);
    List<SampleRecord> findAllByOrderByCreatedAtDesc();
}
