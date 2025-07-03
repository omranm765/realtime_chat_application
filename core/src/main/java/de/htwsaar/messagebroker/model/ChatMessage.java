package de.htwsaar.messagebroker.model;

public class ChatMessage {
    private Long senderId;
    private String senderDisplayName;
    private String eventType;
    private String content;
    private long timestamp;

    public ChatMessage() {}

    public ChatMessage(Long senderId, String senderDisplayName, String eventType, String content, long timestamp) {
        this.senderId = senderId;
        this.senderDisplayName = senderDisplayName;
        this.eventType = eventType;
        this.content = content;
        this.timestamp = timestamp;
    }

    public Long getSenderId() {
        return senderId;
    }


    public void setSenderId(Long senderId) {
        this.senderId = senderId;
    }

    public String getSenderDisplayName() {
        return senderDisplayName;
    }

    public void setSenderDisplayName(String senderDisplayName) {
        this.senderDisplayName = senderDisplayName;
    }

    public String getEventType() {
        return eventType;
    }

    public void setEventType(String eventType) {
        this.eventType = eventType;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }
}
