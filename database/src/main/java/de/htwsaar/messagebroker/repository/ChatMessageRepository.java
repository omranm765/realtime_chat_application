package de.htwsaar.messagebroker.repository;

import de.htwsaar.messagebroker.entity.ChatMessageEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ChatMessageRepository extends JpaRepository<ChatMessageEntity, Long> {
    List<ChatMessageEntity> findByEventTypeOrderByTimestampAsc(String eventType);

    void deleteByEventType(String eventType);
}
