package de.htwsaar.messagebroker.client;

import de.htwsaar.messagebroker.model.ChatMessage;
import org.springframework.messaging.simp.stomp.StompFrameHandler;
import org.springframework.messaging.simp.stomp.StompHeaders;
import org.springframework.web.client.RestTemplate;

import java.lang.reflect.Type;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;

public class MessageHandler implements StompFrameHandler {

    private final String topic;
    private final String clientId; // <-- Hinzugefügt

    public MessageHandler(String topic, String clientId) {
        this.topic = topic;
        this.clientId = clientId;
    }

    @Override
    public Type getPayloadType(StompHeaders headers) {
        return ChatMessage.class;
    }

    @Override
    public void handleFrame(StompHeaders headers, Object payload) {
        ChatMessage message = (ChatMessage) payload;
        String formattedTime = formatTimestamp(message.getTimestamp());
        System.out.println("[" + topic + "] [" + formattedTime + "] " + message.getSenderDisplayName() + ": " + message.getContent());
        try {
            RestTemplate rest = new RestTemplate();
            rest.postForObject(
                    "http://localhost:8080/api/messages/mark-delivered?clientId=" + clientId,
                    List.of(message.getId()), // wichtig: Liste
                    Void.class
            );
        } catch (Exception e) {
            System.out.println("Fehler beim Markieren als zugestellt: " + e.getMessage());
        }

    }

    public static String formatTimestamp(long timestamp) {
        return Instant.ofEpochMilli(timestamp)
                .atZone(ZoneId.systemDefault())
                .format(DateTimeFormatter.ofPattern("HH:mm"));
    }
}
