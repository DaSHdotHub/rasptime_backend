package io.github.dashdothub.rasptime_backend.controller;

import io.github.dashdothub.rasptime_backend.dto.PunchRequest;
import io.github.dashdothub.rasptime_backend.dto.PunchResponse;
import io.github.dashdothub.rasptime_backend.dto.UserStatusResponse;
import io.github.dashdothub.rasptime_backend.entity.AuditAction;
import io.github.dashdothub.rasptime_backend.service.AuditService;
import io.github.dashdothub.rasptime_backend.service.TerminalService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/terminal")
@RequiredArgsConstructor
public class TerminalController {

    private final TerminalService terminalService;
    private final AuditService auditService;

    @GetMapping("/user")
    public ResponseEntity<UserStatusResponse> getUser(@RequestParam String rfid) {
        return terminalService.getUserByRfid(rfid)
                .map(ResponseEntity::ok)
                .orElseGet(() -> {
                    auditService.log(AuditAction.UNKNOWN_RFID, null, rfid, "Unknown RFID attempt");
                    return ResponseEntity.notFound().build();
                });
    }

    @PostMapping("/punch")
    public ResponseEntity<PunchResponse> punch(@RequestBody PunchRequest request) {
        try {
            PunchResponse response = terminalService.punch(request);
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        }
    }
}