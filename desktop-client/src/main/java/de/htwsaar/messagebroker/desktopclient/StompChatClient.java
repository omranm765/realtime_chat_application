package de.htwsaar.messagebroker.desktopclient;

import de.htwsaar.messagebroker.model.ChatMessage;
import org.springframework.messaging.converter.MappingJackson2MessageConverter;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompFrameHandler;
import org.springframework.messaging.simp.stomp.StompHeaders;
import org.springframework.messaging.simp.stomp.StompSession;
import org.springframework.messaging.simp.stomp.StompSessionHandlerAdapter;
import org.springframework.web.socket.WebSocketHttpHeaders;
import org.springframework.web.socket.client.standard.StandardWebSocketClient;
import org.springframework.web.socket.messaging.WebSocketStompClient;

import java.lang.reflect.Type;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

public class StompChatClient {
    private final Consumer<ChatMessage> messageConsumer;
    private final Consumer<String> statusConsumer;
    private final Map<String, StompSession.Subscription> subscriptions = new ConcurrentHashMap<>();

    private WebSocketStompClient stompClient;
    private StompSession session;

    public StompChatClient(Consumer<ChatMessage> messageConsumer, Consumer<String> statusConsumer) {
        this.messageConsumer = messageConsumer;
        this.statusConsumer = statusConsumer;
    }

    public void connect(String webSocketUrl) throws Exception {
        disconnect();

        stompClient = new WebSocketStompClient(new StandardWebSocketClient());
        stompClient.setMessageConverter(new MappingJackson2MessageConverter());

        StompSessionHandlerAdapter handler = new StompSessionHandlerAdapter() {
            @Override
            public void afterConnected(StompSession connectedSession, StompHeaders connectedHeaders) {
                statusConsumer.accept("WebSocket verbunden");
            }

            @Override
            public void handleException(StompSession currentSession, StompCommand command,
                                        StompHeaders headers, byte[] payload, Throwable exception) {
                statusConsumer.accept("WebSocket-Fehler: " + exception.getMessage());
            }

            @Override
            public void handleTransportError(StompSession currentSession, Throwable exception) {
                statusConsumer.accept("WebSocket getrennt: " + exception.getMessage());
            }
        };

        session = stompClient.connectAsync(
                        webSocketUrl,
                        new WebSocketHttpHeaders(),
                        new StompHeaders(),
                        handler)
                .get(8, TimeUnit.SECONDS);
    }

    public boolean isConnected() {
        return session != null && session.isConnected();
    }

    public void subscribe(String eventName) {
        if (!isConnected() || subscriptions.containsKey(eventName)) {
            return;
        }

        StompSession.Subscription subscription = session.subscribe("/topic/" + eventName, new StompFrameHandler() {
            @Override
            public Type getPayloadType(StompHeaders headers) {
                return ChatMessage.class;
            }

            @Override
            public void handleFrame(StompHeaders headers, Object payload) {
                if (payload instanceof ChatMessage chatMessage) {
                    messageConsumer.accept(chatMessage);
                }
            }
        });
        subscriptions.put(eventName, subscription);
        statusConsumer.accept("Live-Abo aktiv: " + eventName);
    }

    public void unsubscribe(String eventName) {
        StompSession.Subscription subscription = subscriptions.remove(eventName);
        if (subscription != null) {
            subscription.unsubscribe();
            statusConsumer.accept("Live-Abo beendet: " + eventName);
        }
    }

    public void send(ChatMessage message) {
        if (!isConnected()) {
            throw new IllegalStateException("WebSocket ist nicht verbunden");
        }
        session.send("/app/chat/send", message);
    }

    public void disconnect() {
        for (StompSession.Subscription subscription : subscriptions.values()) {
            subscription.unsubscribe();
        }
        subscriptions.clear();

        if (session != null && session.isConnected()) {
            session.disconnect();
        }
        session = null;

        if (stompClient != null) {
            stompClient.stop();
        }
        stompClient = null;
    }
}
