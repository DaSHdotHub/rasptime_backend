package io.github.dashdothub.rasptime_backend.service;

import io.github.dashdothub.rasptime_backend.entity.AuditAction;
import io.github.dashdothub.rasptime_backend.entity.AuditLog;
import io.github.dashdothub.rasptime_backend.repository.AuditLogRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuditService {

    private final AuditLogRepository auditLogRepository;

    public void log(AuditAction action, Long userId, String rfid, String details) {
        AuditLog entry = AuditLog.builder()
                .action(action)
                .userId(userId)
                .rfidUsed(rfid)
                .details(details)
                .build();
        auditLogRepository.save(entry);
    }
}