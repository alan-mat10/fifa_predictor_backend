package com.fifaworldcup.Fifa.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

@RestController
@RequestMapping("/api/announcement")
public class AnnouncementController {

    private static final AtomicReference<String> pinnedMessage = new AtomicReference<>(null);

    @GetMapping
    public ResponseEntity<Map<String, String>> getAnnouncement() {
        String msg = pinnedMessage.get();
        if (msg == null || msg.isBlank()) {
            return ResponseEntity.ok(Map.of());
        }
        return ResponseEntity.ok(Map.of("message", msg));
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, String>> setAnnouncement(@RequestBody Map<String, String> body) {
        String msg = body.get("message");
        pinnedMessage.set(msg);
        return ResponseEntity.ok(Map.of("message", msg != null ? msg : ""));
    }

    @DeleteMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<String> clearAnnouncement() {
        pinnedMessage.set(null);
        return ResponseEntity.ok("Announcement cleared");
    }
}
