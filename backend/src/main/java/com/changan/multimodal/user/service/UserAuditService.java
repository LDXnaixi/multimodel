package com.changan.multimodal.user.service;

import com.changan.multimodal.common.persistence.DemoPersistenceService;
import com.changan.multimodal.user.dto.LoginRequest;
import com.changan.multimodal.user.dto.LoginStatRecord;
import com.changan.multimodal.user.dto.LoginSummary;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class UserAuditService {

    private static final String DOMAIN = "USER";
    private static final String TYPE_LOGIN = "LOGIN_STAT";

    private final CopyOnWriteArrayList<LoginStatRecord> records = new CopyOnWriteArrayList<>();
    private final DemoPersistenceService persistenceService;

    public LoginStatRecord recordLogin(LoginRequest request) {
        LoginStatRecord record = LoginStatRecord.builder()
                .username(request.getUsername())
                .ip(StringUtils.hasText(request.getIp()) ? request.getIp() : "unknown")
                .module(request.getModule())
                .action("LOGIN")
                .loginTime(Instant.now().toEpochMilli())
                .build();
        records.add(0, record);
        persistenceService.save(DOMAIN, TYPE_LOGIN, UUID.randomUUID().toString().replace("-", ""), record);
        trim();
        return record;
    }

    public void recordAccess(String username, String ip, String module, String action) {
        LoginStatRecord record = LoginStatRecord.builder()
                .username(StringUtils.hasText(username) ? username : "anonymous")
                .ip(ip)
                .module(module)
                .action(action)
                .loginTime(Instant.now().toEpochMilli())
                .build();
        records.add(0, record);
        persistenceService.save(DOMAIN, TYPE_LOGIN, UUID.randomUUID().toString().replace("-", ""), record);
        trim();
    }

    public List<LoginStatRecord> listRecords() {
        if (records.isEmpty()) {
            records.addAll(persistenceService.findAll(DOMAIN, TYPE_LOGIN, LoginStatRecord.class));
        }
        return List.copyOf(records);
    }

    public LoginSummary summary() {
        List<LoginStatRecord> current = listRecords();
        Map<String, Long> moduleAccessCount = current.stream()
                .collect(Collectors.groupingBy(LoginStatRecord::getModule, Collectors.counting()));
        long uniqueUsers = current.stream().map(LoginStatRecord::getUsername).distinct().count();
        return LoginSummary.builder()
                .totalCount(current.size())
                .uniqueUsers((int) uniqueUsers)
                .moduleAccessCount(moduleAccessCount)
                .build();
    }

    private void trim() {
        while (records.size() > 300) {
            records.remove(records.size() - 1);
        }
    }
}
