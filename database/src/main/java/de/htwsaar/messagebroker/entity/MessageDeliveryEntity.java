package de.htwsaar.messagebroker.entity;

import de.htwsaar.messagebroker.repository.MessageDeliveryRepository;
import jakarta.persistence.*;

@Entity
@Table(name = "message_delivery")
public class MessageDeliveryEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long messageId;

    private String clientId;

    private long deliveredAt; // Timestamp

    public MessageDeliveryEntity(){

    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getMessageId() {
        return messageId;
    }

    public void setMessageId(Long messageId) {
        this.messageId = messageId;
    }

    public String getClientId() {
        return clientId;
    }

    public void setClientId(String clientId) {
        this.clientId = clientId;
    }

    public long getDeliveredAt() {
        return deliveredAt;
    }

    public void setDeliveredAt(long deliveredAt) {
        this.deliveredAt = deliveredAt;
    }
}
