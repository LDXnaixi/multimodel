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
@Table(name = "data_datasets", indexes = {
        @Index(name = "idx_data_dataset_updated", columnList = "updated_at")
})
public class DatasetRecord {

    @Id
    @Column(name = "dataset_id", length = 32)
    private String datasetId;

    @Column(name = "dataset_name", nullable = false, length = 255)
    private String datasetName;

    @Column(name = "asset_count", nullable = false)
    private int assetCount;

    @Lob
    @Column(name = "supported_modalities_json", nullable = false, columnDefinition = "LONGTEXT")
    private String supportedModalitiesJson;

    @Column(nullable = false, length = 32)
    private String version;

    @Column(nullable = false, length = 32)
    private String status;

    @Column(name = "created_at", nullable = false)
    private long createdAt;

    @Column(name = "updated_at", nullable = false)
    private long updatedAt;
}
