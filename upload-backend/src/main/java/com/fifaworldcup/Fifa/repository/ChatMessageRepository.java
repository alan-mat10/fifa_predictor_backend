package com.fifaworldcup.Fifa.repository;

import com.fifaworldcup.Fifa.model.ChatMessage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface ChatMessageRepository extends JpaRepository<ChatMessage, Long> {

    // Community messages — latest 100
    List<ChatMessage> findTop100ByTypeOrderBySentAtDesc(ChatMessage.ChatType type);

    // Private messages between two users
    @Query("SELECT m FROM ChatMessage m WHERE m.type = 'PRIVATE' AND " +
           "((m.senderUsername = :user1 AND m.recipientUsername = :user2) OR " +
           "(m.senderUsername = :user2 AND m.recipientUsername = :user1)) " +
           "ORDER BY m.sentAt DESC")
    List<ChatMessage> findPrivateMessages(@Param("user1") String user1, @Param("user2") String user2);

    // Community messages after a given ID (for polling)
    List<ChatMessage> findByTypeAndIdGreaterThanOrderBySentAtAsc(ChatMessage.ChatType type, Long afterId);

    // Private messages after a given ID (for polling)
    @Query("SELECT m FROM ChatMessage m WHERE m.type = 'PRIVATE' AND m.id > :afterId AND " +
           "((m.senderUsername = :user1 AND m.recipientUsername = :user2) OR " +
           "(m.senderUsername = :user2 AND m.recipientUsername = :user1)) " +
           "ORDER BY m.sentAt ASC")
    List<ChatMessage> findNewPrivateMessages(@Param("user1") String user1, @Param("user2") String user2, @Param("afterId") Long afterId);
}
