package com.changan.multimodal.common.persistence;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class DemoPersistenceService {

    private final DemoRecordRepository repository;
    private final ObjectMapper objectMapper;

    public <T> T save(String domainName, String recordType, String objectId, T payload) {
        try {
            long now = Instant.now().toEpochMilli();
            DemoRecord record = repository
                    .findFirstByDomainNameAndRecordTypeAndObjectId(domainName, recordType, objectId)
                    .orElseGet(DemoRecord::new);
            if (record.getRecordId() == null) {
                record.setRecordId(UUID.randomUUID().toString().replace("-", ""));
                record.setCreatedAt(now);
            }
            record.setDomainName(domainName);
            record.setRecordType(recordType);
            record.setObjectId(objectId);
            record.setPayloadJson(objectMapper.writeValueAsString(payload));
            record.setUpdatedAt(now);
            repository.save(record);
            return payload;
        } catch (Exception ex) {
            throw new IllegalStateException("持久化演示记录失败: " + domainName + "/" + recordType + "/" + objectId, ex);
        }
    }

    public <T> Optional<T> findOne(String domainName, String recordType, String objectId, Class<T> type) {
        return repository.findFirstByDomainNameAndRecordTypeAndObjectId(domainName, recordType, objectId)
                .map(record -> convert(record, type));
    }

    public <T> List<T> findAll(String domainName, String recordType, Class<T> type) {
        return repository.findByDomainNameAndRecordTypeOrderByUpdatedAtDesc(domainName, recordType).stream()
                .map(record -> convert(record, type))
                .toList();
    }

    public boolean exists(String domainName, String recordType) {
        return !repository.findByDomainNameAndRecordTypeOrderByUpdatedAtDesc(domainName, recordType).isEmpty();
    }

    public void delete(String domainName, String recordType, String objectId) {
        repository.deleteByDomainNameAndRecordTypeAndObjectId(domainName, recordType, objectId);
    }

    private <T> T convert(DemoRecord record, Class<T> type) {
        try {
            return objectMapper.readValue(record.getPayloadJson(), type);
        } catch (Exception ex) {
            throw new IllegalStateException("读取演示记录失败: " + record.getDomainName() + "/" + record.getRecordType(), ex);
        }
    }
}
