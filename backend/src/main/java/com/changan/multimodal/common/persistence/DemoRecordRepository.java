package com.changan.multimodal.common.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface DemoRecordRepository extends JpaRepository<DemoRecord, String> {

    List<DemoRecord> findByDomainNameAndRecordTypeOrderByUpdatedAtDesc(String domainName, String recordType);

    Optional<DemoRecord> findFirstByDomainNameAndRecordTypeAndObjectId(String domainName, String recordType, String objectId);

    List<DemoRecord> findByDomainNameAndObjectIdOrderByUpdatedAtDesc(String domainName, String objectId);

    void deleteByDomainNameAndRecordTypeAndObjectId(String domainName, String recordType, String objectId);
}
