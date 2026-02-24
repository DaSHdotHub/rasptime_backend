// RegistrationService.java (new file)
package io.github.dashdothub.rasptime_backend.service;

import org.springframework.stereotype.Service;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class RegistrationService {

    private final Map<String, RegistrationSession> sessions = new ConcurrentHashMap<>();

    public String startSession() {
        // Clean old sessions
        sessions.entrySet().removeIf(e ->
                e.getValue().expiresAt.isBefore(LocalDateTime.now()));

        String sessionId = UUID.randomUUID().toString();
        sessions.put(sessionId, new RegistrationSession(
                LocalDateTime.now().plusSeconds(30)
        ));
        return sessionId;
    }

    public Map<String, Object> getStatus(String sessionId) {
        RegistrationSession session = sessions.get(sessionId);
        if (session == null) {
            return Map.of("status", "expired");
        }
        if (session.expiresAt.isBefore(LocalDateTime.now())) {
            sessions.remove(sessionId);
            return Map.of("status", "expired");
        }
        if (session.rfidTag != null) {
            sessions.remove(sessionId);
            return Map.of(
                    "status", "completed",
                    "rfidTag", session.rfidTag
            );
        }
        return Map.of(
                "status", "waiting",
                "expiresAt", session.expiresAt.toString()
        );
    }

    public void submitRfid(String sessionId, String rfidTag) {
        RegistrationSession session = sessions.get(sessionId);
        if (session != null && session.expiresAt.isAfter(LocalDateTime.now())) {
            session.rfidTag = rfidTag;
        }
    }

    public boolean hasActiveSession() {
        return sessions.values().stream()
                .anyMatch(s -> s.expiresAt.isAfter(LocalDateTime.now()) && s.rfidTag == null);
    }

    public String getActiveSessionId() {
        return sessions.entrySet().stream()
                .filter(e -> e.getValue().expiresAt.isAfter(LocalDateTime.now()) && e.getValue().rfidTag == null)
                .map(Map.Entry::getKey)
                .findFirst()
                .orElse(null);
    }

    private static class RegistrationSession {
        LocalDateTime expiresAt;
        String rfidTag;

        RegistrationSession(LocalDateTime expiresAt) {
            this.expiresAt = expiresAt;
        }
    }
}