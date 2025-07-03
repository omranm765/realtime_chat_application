package de.htwsaar.messagebroker.client;

import de.htwsaar.messagebroker.entity.ChatMessageEntity;
import de.htwsaar.messagebroker.entity.UserEntity;
import de.htwsaar.messagebroker.model.ChatMessage;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.converter.MappingJackson2MessageConverter;
import org.springframework.messaging.simp.stomp.StompSession;
import org.springframework.messaging.simp.stomp.StompSessionHandlerAdapter;
import org.springframework.messaging.simp.stomp.StompSession.Subscription;
import org.springframework.util.concurrent.ListenableFuture;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.socket.WebSocketHttpHeaders;
import org.springframework.web.socket.client.standard.StandardWebSocketClient;
import org.springframework.web.socket.messaging.WebSocketStompClient;

import java.util.*;

public class WebSocketClientEndpoint {

    private StompSession session;
    private final UserEntity user;
    private final Map<String, Subscription> subscriptions = new HashMap<>();

    public WebSocketClientEndpoint(String url, UserEntity user) {
        this.user = user;
        try {
            WebSocketStompClient stompClient = new WebSocketStompClient(new StandardWebSocketClient());
            stompClient.setMessageConverter(new MappingJackson2MessageConverter());

            ListenableFuture<StompSession> future = stompClient.connect(url, new WebSocketHttpHeaders(), new StompSessionHandlerAdapter() {
            });
            session = future.get();

            // Automatisch alle gespeicherten Subscriptions wiederherstellen
            restoreSubscriptionsFromDatabase();

        } catch (Exception e) {
            throw new RuntimeException("Verbindung fehlgeschlagen: " + e.getMessage());
        }
    }

    private void restoreSubscriptionsFromDatabase() {
        RestTemplate restTemplate = new RestTemplate();
        String url = "http://localhost:8080/api/clients/" + user.getEmail() + "/subscriptions";
        try {
            String[] topics = restTemplate.getForObject(url, String[].class);
            if (topics != null) {
                for (String topic : topics) {
                    Subscription sub = session.subscribe("/topic/" + topic, new MessageHandler(topic));
                    subscriptions.put(topic, sub);
                }
            }
        } catch (Exception e) {
            System.out.println("Fehler beim Wiederherstellen der Subscriptions: " + e.getMessage());
        }
    }

    public void subscribe(String topic) {
        if (subscriptions.containsKey(topic)) {
            System.out.println("Bereits zu diesem Event subscribed.");
            return;
        }

        RestTemplate restTemplate = new RestTemplate();

        // Prüfen ob Event existiert
        String eventsUrl = "http://localhost:8080/api/admin/events";
        String[] existingEvents = restTemplate.getForObject(eventsUrl, String[].class);
        if (existingEvents == null || !Arrays.asList(existingEvents).contains(topic)) {
            System.out.println("Event '" + topic + "' existiert nicht.");
            return;
        }

        // Alte Nachrichten laden
        String url = "http://localhost:8080/api/messages/" + topic;
        ResponseEntity<ChatMessageEntity[]> response = restTemplate.getForEntity(url, ChatMessageEntity[].class);
        if (response.getBody() != null) {
            for (ChatMessageEntity msg : response.getBody()) {
                System.out.println("[History][" + topic + "] " + msg.getSenderDisplayName() + ": " + msg.getContent());
            }
        }

        // WebSocket abonnieren
        Subscription sub = session.subscribe("/topic/" + topic, new MessageHandler(topic));
        subscriptions.put(topic, sub);

        // DB-Abo speichern
        String subscribeUrl = "http://localhost:8080/api/clients/" + user.getEmail() + "/subscribe/" + topic;
        try {
            restTemplate.postForObject(subscribeUrl, null, Void.class);
        } catch (Exception e) {
            System.out.println("Konnte Subscription nicht speichern: " + e.getMessage());
        }

        System.out.println("Subscribed to event: " + topic);
    }

    public void unsubscribe() {
        if (subscriptions.isEmpty()) {
            System.out.println("Keine aktiven Subscriptions zum Abmelden.");
            return;
        }

        System.out.println("Aktive Subscriptions:");
        for (String topic : subscriptions.keySet()) {
            System.out.println(" - " + topic);
        }

        System.out.print("Welches Topic moechtest du abmelden? > ");
        Scanner scanner = new Scanner(System.in);
        String topic = scanner.nextLine().trim();

        Subscription sub = subscriptions.remove(topic);
        if (sub != null) {
            sub.unsubscribe();
            System.out.println("Unsubscribed von " + topic);

            String unsubscribeUrl = "http://localhost:8080/api/clients/" + user.getEmail() + "/unsubscribe/" + topic;
            try {
                RestTemplate restTemplate = new RestTemplate();
                restTemplate.delete(unsubscribeUrl);
            } catch (Exception e) {
                System.out.println("Konnte DB-Subscription nicht entfernen: " + e.getMessage());
            }
        } else {
            System.out.println("Nicht gefunden.");
        }
    }

    public void publish(String topic, String message) {
        ChatMessage payload = new ChatMessage(user.getId(), user.getDisplayName(), topic, message, System.currentTimeMillis());
        if (!subscriptions.containsKey(topic)) {
            System.out.println("Nicht subscribed. Automatisch subscriben...");
            System.out.println("willst du zu diesem Event subscribed werden? (ja/nein)");
            if (!"ja".equalsIgnoreCase(new Scanner(System.in).nextLine().trim())) {
                System.out.println("Nachricht nicht gesendet.");
                return;
            } else {
                subscribe(topic);
            }
        }
        session.send("/app/chat/send", payload);
    }

    public void multi_publish(String topic) {
        Scanner scanner = new Scanner(System.in);
        System.out.println("Gib Nachrichten ein (eine pro Zeile). Schreibe 'exit', um zu beenden:");

        while (true) {
            System.out.print("Nachricht > ");
            String line = scanner.nextLine().trim();
            if ("exit".equalsIgnoreCase(line)) {
                System.out.println("Broadcast beendet.");
                break;
            }
            publish(topic, line);
        }
    }


    public void listEventTypes() {
        RestTemplate restTemplate = new RestTemplate();
        String url = "http://localhost:8080/api/admin/events";
        String[] events = restTemplate.getForObject(url, String[].class);
        System.out.println("Available event types:");
        for (String event : events) {
            System.out.println(" - " + event);
        }
    }

    public void listMySubscribedEvents() {
        String url = "http://localhost:8080/api/clients/" + user.getEmail() + "/subscriptions";
        RestTemplate restTemplate = new RestTemplate();
        try {
            String[] subscribedTopics = restTemplate.getForObject(url, String[].class);
            if (subscribedTopics == null || subscribedTopics.length == 0) {
                System.out.println("Du bist zu keinem Event subscribed.");
            } else {
                System.out.println("Du bist subscribed zu:");
                for (String topic : subscribedTopics) {
                    System.out.println(" - " + topic);
                }
            }
        } catch (Exception e) {
            System.out.println("Fehler beim Laden der Subscriptions: " + e.getMessage());
        }
    }

    public void disconnect() {
        for (Subscription sub : subscriptions.values()) {
            sub.unsubscribe();
        }
        subscriptions.clear();

        if (session != null && session.isConnected()) {
            session.disconnect();
        }
    }
}
