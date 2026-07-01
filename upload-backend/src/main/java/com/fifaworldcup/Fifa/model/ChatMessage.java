package com.fifaworldcup.Fifa.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "chat_messages")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ChatMessage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 500)
    private String message;

    @Column(nullable = false)
    private String senderUsername;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ChatType type;

    // For PRIVATE messages: the other participant (user or admin)
    private String recipientUsername;

    @Builder.Default
    private LocalDateTime sentAt = LocalDateTime.now();

    public enum ChatType {
        COMMUNITY, PRIVATE
    }
}
