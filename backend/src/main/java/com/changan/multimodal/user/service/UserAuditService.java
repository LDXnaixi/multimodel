package com.changan.multimodal.user.service;

import com.changan.multimodal.user.dto.LoginRequest;
import com.changan.multimodal.user.dto.LoginStatRecord;
import com.changan.multimodal.user.dto.LoginSummary;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class UserAuditService {

    private final CopyOnWriteArrayList<LoginStatRecord> records = new CopyOnWriteArrayList<>();

    public LoginStatRecord recordLogin(LoginRequest request) {
        LoginStatRecord record = LoginStatRecord.builder()
                .username(request.getUsername())
                .ip(StringUtils.hasText(request.getIp()) ? request.getIp() : "unknown")
                .module(request.getModule())
                .action("LOGIN")
                .loginTime(Instant.now().toEpochMilli())
                .build();
        records.add(0, record);
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
        trim();
    }

    public List<LoginStatRecord> listRecords() {
        return List.copyOf(records);
    }

    public LoginSummary summary() {
        Map<String, Long> moduleAccessCount = records.stream()
                .collect(Collectors.groupingBy(LoginStatRecord::getModule, Collectors.counting()));
        long uniqueUsers = records.stream().map(LoginStatRecord::getUsername).distinct().count();
        return LoginSummary.builder()
                .totalCount(records.size())
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
