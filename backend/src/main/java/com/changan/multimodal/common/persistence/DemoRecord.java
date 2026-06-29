package com.changan.multimodal.common.persistence;

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
@Table(name = "demo_records", indexes = {
        @Index(name = "idx_demo_record_domain", columnList = "domain_name,record_type"),
        @Index(name = "idx_demo_record_object", columnList = "object_id")
})
public class DemoRecord {

    @Id
    @Column(length = 80)
    private String recordId;

    @Column(name = "domain_name", nullable = false, length = 40)
    private String domainName;

    @Column(name = "record_type", nullable = false, length = 60)
    private String recordType;

    @Column(name = "object_id", nullable = false, length = 120)
    private String objectId;

    @Lob
    @Column(nullable = false, columnDefinition = "LONGTEXT")
    private String payloadJson;

    @Column(nullable = false)
    private long createdAt;

    @Column(nullable = false)
    private long updatedAt;
}

