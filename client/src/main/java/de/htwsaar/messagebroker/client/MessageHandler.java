package de.htwsaar.messagebroker.client;

import de.htwsaar.messagebroker.model.ChatMessage;
import org.springframework.messaging.simp.stomp.StompFrameHandler;
import org.springframework.messaging.simp.stomp.StompHeaders;

import java.lang.reflect.Type;

public class MessageHandler implements StompFrameHandler {

    private final String topic;

    public MessageHandler(String topic) {
        this.topic = topic;
    }

    @Override
    public Type getPayloadType(StompHeaders headers) {
        return ChatMessage.class;
    }

    @Override
    public void handleFrame(StompHeaders headers, Object payload) {
        ChatMessage message = (ChatMessage) payload;
        System.out.println("[" + topic + "] " + message.getSenderDisplayName() + ": " + message.getContent());
    }
}
