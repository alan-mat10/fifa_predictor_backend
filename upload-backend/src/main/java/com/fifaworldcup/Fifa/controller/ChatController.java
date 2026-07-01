package com.fifaworldcup.Fifa.controller;

import com.fifaworldcup.Fifa.model.ChatMessage;
import com.fifaworldcup.Fifa.repository.ChatMessageRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.*;

@RestController
@RequestMapping("/api/chat")
@RequiredArgsConstructor
public class ChatController {

    private final ChatMessageRepository chatMessageRepository;

    /**
     * Get community messages (latest 100, reversed to chronological order)
     */
    @GetMapping("/community")
    public ResponseEntity<List<Map<String, Object>>> getCommunityMessages(
            @RequestParam(required = false) Long afterId) {
        List<ChatMessage> messages;
        if (afterId != null && afterId > 0) {
            messages = chatMessageRepository.findByTypeAndIdGreaterThanOrderBySentAtAsc(
                    ChatMessage.ChatType.COMMUNITY, afterId);
        } else {
            messages = chatMessageRepository.findTop100ByTypeOrderBySentAtDesc(ChatMessage.ChatType.COMMUNITY);
            Collections.reverse(messages);
        }
        return ResponseEntity.ok(messages.stream().map(this::toResponse).toList());
    }

    /**
     * Send a community message
     */
    @PostMapping("/community")
    public ResponseEntity<Map<String, Object>> sendCommunityMessage(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestBody Map<String, String> body) {
        String message = body.get("message");
        if (message == null || message.trim().isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("error", "Message cannot be empty"));
        }
        if (message.length() > 500) {
            return ResponseEntity.badRequest().body(Map.of("error", "Message cannot exceed 500 characters"));
        }

        ChatMessage chatMessage = ChatMessage.builder()
                .message(message.trim())
                .senderUsername(userDetails.getUsername())
                .type(ChatMessage.ChatType.COMMUNITY)
                .sentAt(LocalDateTime.now())
                .build();
        chatMessageRepository.save(chatMessage);
        return ResponseEntity.ok(toResponse(chatMessage));
    }

    /**
     * Get private messages with admin
     */
    @GetMapping("/private")
    public ResponseEntity<List<Map<String, Object>>> getPrivateMessages(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestParam(required = false) Long afterId,
            @RequestParam(required = false) String withUser) {
        String currentUser = userDetails.getUsername();
        String otherUser = withUser != null ? withUser : "admin";

        List<ChatMessage> messages;
        if (afterId != null && afterId > 0) {
            messages = chatMessageRepository.findNewPrivateMessages(currentUser, otherUser, afterId);
        } else {
            messages = chatMessageRepository.findPrivateMessages(currentUser, otherUser);
            Collections.reverse(messages);
        }
        return ResponseEntity.ok(messages.stream().map(this::toResponse).toList());
    }

    /**
     * Send a private message to admin (or admin to user)
     */
    @PostMapping("/private")
    public ResponseEntity<Map<String, Object>> sendPrivateMessage(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestBody Map<String, String> body) {
        String message = body.get("message");
        String recipient = body.get("recipient");

        if (message == null || message.trim().isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("error", "Message cannot be empty"));
        }
        if (message.length() > 500) {
            return ResponseEntity.badRequest().body(Map.of("error", "Message cannot exceed 500 characters"));
        }
        if (recipient == null || recipient.trim().isEmpty()) {
            recipient = "admin"; // Default recipient is admin
        }

        ChatMessage chatMessage = ChatMessage.builder()
                .message(message.trim())
                .senderUsername(userDetails.getUsername())
                .recipientUsername(recipient.trim())
                .type(ChatMessage.ChatType.PRIVATE)
                .sentAt(LocalDateTime.now())
                .build();
        chatMessageRepository.save(chatMessage);
        return ResponseEntity.ok(toResponse(chatMessage));
    }

    /**
     * Admin: get list of users who have sent private messages
     */
    @GetMapping("/private/users")
    public ResponseEntity<List<String>> getPrivateMessageUsers(
            @AuthenticationPrincipal UserDetails userDetails) {
        // Only meaningful for admin
        if (!userDetails.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"))) {
            return ResponseEntity.ok(List.of("admin"));
        }
        List<ChatMessage> allPrivate = chatMessageRepository.findTop100ByTypeOrderBySentAtDesc(ChatMessage.ChatType.PRIVATE);
        Set<String> users = new LinkedHashSet<>();
        for (ChatMessage m : allPrivate) {
            if (!"admin".equals(m.getSenderUsername())) users.add(m.getSenderUsername());
            if (m.getRecipientUsername() != null && !"admin".equals(m.getRecipientUsername())) users.add(m.getRecipientUsername());
        }
        return ResponseEntity.ok(new ArrayList<>(users));
    }

    private Map<String, Object> toResponse(ChatMessage m) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("id", m.getId());
        map.put("message", m.getMessage());
        map.put("sender", m.getSenderUsername());
        map.put("type", m.getType().name());
        map.put("sentAt", m.getSentAt().toString());
        if (m.getRecipientUsername() != null) {
            map.put("recipient", m.getRecipientUsername());
        }
        return map;
    }
}
