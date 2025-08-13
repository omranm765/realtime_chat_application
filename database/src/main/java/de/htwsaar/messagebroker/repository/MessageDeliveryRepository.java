package de.htwsaar.messagebroker.repository;

import de.htwsaar.messagebroker.entity.MessageDeliveryEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface MessageDeliveryRepository extends JpaRepository<MessageDeliveryEntity, Long> {
    List<MessageDeliveryEntity> findByClientId(String clientId);

    boolean existsByClientIdAndMessageId(String clientId, Long messageId);
}
