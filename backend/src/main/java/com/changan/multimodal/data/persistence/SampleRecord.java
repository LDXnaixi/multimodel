package com.changan.multimodal.data.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Lob;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "data_samples", indexes = {
        @Index(name = "idx_data_sample_dataset", columnList = "dataset_id"),
        @Index(name = "idx_data_sample_type", columnList = "data_type"),
        @Index(name = "idx_data_sample_sha256", columnList = "sha256")
})
public class SampleRecord {

    @Id
    @Column(name = "sample_id", length = 32)
    private String sampleId;

    @Column(name = "dataset_id", nullable = false, length = 32)
    private String datasetId;

    @Column(nullable = false, length = 255)
    private String name;

    @Column(name = "data_type", nullable = false, length = 32)
    private String dataType;

    @Column(name = "storage_key", nullable = false, length = 600)
    private String storageKey;

    @Column(name = "image_relative_path", length = 600)
    private String imageRelativePath;

    @Column(name = "original_name", nullable = false, length = 255)
    private String originalName;

    @Column(name = "content_type", length = 150)
    private String contentType;

    @Column(name = "file_size", nullable = false)
    private long fileSize;

    @Column(length = 64)
    private String sha256;

    @Column(name = "label_storage_key", length = 600)
    private String labelStorageKey;

    @Column(name = "label_relative_path", length = 600)
    private String labelRelativePath;

    @Column(name = "label_original_name", length = 255)
    private String labelOriginalName;

    @Column(name = "label_content_type", length = 150)
    private String labelContentType;

    @Column(name = "label_file_size")
    private Long labelFileSize;

    @Column(name = "label_sha256", length = 64)
    private String labelSha256;

    @Lob
    @Column(name = "tags_json", nullable = false, columnDefinition = "LONGTEXT")
    private String tagsJson;

    @Lob
    @Column(name = "metadata_json", nullable = false, columnDefinition = "LONGTEXT")
    private String metadataJson;

    @Column(nullable = false, length = 32)
    private String version;

    @Column(name = "created_at", nullable = false)
    private long createdAt;

    @Column(name = "updated_at", nullable = false)
    private long updatedAt;
}
