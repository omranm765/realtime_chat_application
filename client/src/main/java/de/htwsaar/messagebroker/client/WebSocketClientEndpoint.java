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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.*;

public class WebSocketClientEndpoint {

    private StompSession session;
    private final UserEntity user;
    private final Map<String, Subscription> subscriptions = new HashMap<>();
    private static final Logger log = LoggerFactory.getLogger(WebSocketClientEndpoint.class);


    public WebSocketClientEndpoint(String url, UserEntity user) {
        this.user = user;
        try {
            WebSocketStompClient stompClient = new WebSocketStompClient(new StandardWebSocketClient());
            stompClient.setMessageConverter(new MappingJackson2MessageConverter());

            ListenableFuture<StompSession> future = stompClient.connect(url, new WebSocketHttpHeaders(), new StompSessionHandlerAdapter() {
            });
            session = future.get();
            log.info("WebSocket-Verbindung hergestellt für Benutzer: {}", user.getEmail());

            // Automatisch alle gespeicherten Subscriptions wiederherstellen
            restoreSubscriptionsFromDatabase();

        } catch (Exception e) {
            log.error("WebSocket-Verbindung fehlgeschlagen: {}", e.getMessage());
            throw new RuntimeException("Verbindung fehlgeschlagen: " + e.getMessage());
        }
    }

    private void restoreSubscriptionsFromDatabase() {
        log.info("Wiederherstellen von Subscriptions für {}", user.getEmail());
        RestTemplate restTemplate = new RestTemplate();
        String url = "http://localhost:8080/api/clients/" + user.getEmail() + "/subscriptions";
        try {
            String[] topics = restTemplate.getForObject(url, String[].class);
            if (topics != null) {
                for (String topic : topics) {
                    Subscription sub = session.subscribe("/topic/" + topic, new MessageHandler(topic, user.getEmail()));
                    subscriptions.put(topic, sub);
                    log.info(" → Topic wiederhergestellt: {}", topic);
                }
            }
        } catch (Exception e) {
            log.error("Fehler beim Wiederherstellen der Subscriptions: {}", e.getMessage());
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
            log.warn("Event '{}' existiert nicht – Abo abgelehnt", topic);

            return;
        }

        // Alte Nachrichten laden
        String url = "http://localhost:8080/api/messages/" + topic;
        ResponseEntity<ChatMessageEntity[]> response = restTemplate.getForEntity(url, ChatMessageEntity[].class);
        if (response.getBody() != null) {
            for (ChatMessageEntity msg : response.getBody()) {
                String formattedTime = formatTimestamp(msg.getTimestamp());
                System.out.println("[History][" + topic + "] [" + formattedTime + "] " + msg.getSenderDisplayName() + ": " + msg.getContent());

            }
        }

        // WebSocket abonnieren
        Subscription sub = session.subscribe("/topic/" + topic, new MessageHandler(topic, user.getEmail()));
        subscriptions.put(topic, sub);
        log.info("Client '{}' hat '{}' abonniert", user.getEmail(), topic);

        // DB-Abo speichern
        String subscribeUrl = "http://localhost:8080/api/clients/" + user.getEmail() + "/subscribe/" + topic;
        try {
            restTemplate.postForObject(subscribeUrl, null, Void.class);
        } catch (Exception e) {
            log.warn("Konnte DB-Subscription nicht speichern: {}", e.getMessage());
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
            log.info("Client '{}' hat '{}' abbestellt", user.getEmail(), topic);

            String unsubscribeUrl = "http://localhost:8080/api/clients/" + user.getEmail() + "/unsubscribe/" + topic;
            try {
                RestTemplate restTemplate = new RestTemplate();
                restTemplate.delete(unsubscribeUrl);
            } catch (Exception e) {
                log.warn("Konnte DB-Subscription nicht entfernen: {}", e.getMessage());
            }
        } else {
            System.out.println("Du bist nicht zu '" + topic + "' subscribed.");
        }
    }

    public void publish(String topic, String message) {
        ChatMessage payload = new ChatMessage(user.getId(), user.getDisplayName(), topic, message, System.currentTimeMillis());
        if (!subscriptions.containsKey(topic)) {
            System.out.println("Nicht subscribed. Automatisch subscriben...");
            System.out.println("willst du zu diesem Event subscribed werden? (ja/nein)");
            if (!"ja".equalsIgnoreCase(new Scanner(System.in).nextLine().trim())) {
                log.info("Nachricht nicht gesendet – Client '{}' wollte nicht subscriben auf '{}'", user.getEmail(), topic);
                return;
            } else {
                subscribe(topic);
            }
        }
        session.send("/app/chat/send", payload);
        log.info("Nachricht veröffentlicht an '{}' von '{}': {}", topic, user.getEmail(), message);
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

    public void multi_subscribe() {
        Scanner scanner = new Scanner(System.in);
        System.out.println("Gib Topic-Namen ein (eine pro Zeile). Schreibe 'exit' zum Beenden.");
        boolean done = false;

        while (!done) {
            String clientTopic;
            String[] availableEvents = listMyUnsubscribedEvents();
            if (availableEvents.length == 0) {
                log.info("Client '{}' ist zu allen verfügbaren Events subscribed", user.getEmail());
                clientTopic = "exit";
                done = true;
            } else {
                System.out.print("Topic > ");
                clientTopic = scanner.nextLine().trim();
            }

            if ("exit".equalsIgnoreCase(clientTopic)) {
                System.out.println("Multi-Subscribe beendet.");
                done = true;
            } else if (clientTopic.isEmpty()) {
                System.out.println("Topic darf nicht leer sein.");
            } else {
                subscribe(clientTopic);
            }
        }
    }


    public void multi_unsubscribe() {
        Scanner scanner = new Scanner(System.in);

        while (true) {
            String clientTopic;

            if (subscriptions.isEmpty()) {
                System.out.println("Keine aktiven Subscriptions zum Abmelden.");
                clientTopic = "exit";
            } else {
                System.out.println("\nAktive Subscriptions:");
                for (String topic : subscriptions.keySet()) {
                    System.out.println(" - " + topic);
                }
                System.out.print("Gib ein Topic zum Abmelden ein (oder 'exit' zum Beenden): ");
                clientTopic = scanner.nextLine().trim();
            }

            if ("exit".equalsIgnoreCase(clientTopic)) {
                System.out.println("Multi-Unsubscribe beendet.");
                break;
            }

            Subscription sub = subscriptions.remove(clientTopic);
            if (sub != null) {
                sub.unsubscribe();
                log.info("Client '{}' hat '{}' abbestellt", user.getEmail(), clientTopic);

                String unsubscribeUrl = "http://localhost:8080/api/clients/" + user.getEmail() + "/unsubscribe/" + clientTopic;
                try {
                    RestTemplate restTemplate = new RestTemplate();
                    restTemplate.delete(unsubscribeUrl);
                } catch (Exception e) {
                    log.warn("Konnte DB-Subscription nicht entfernen: {}", e.getMessage());
                }
            } else {
                System.out.println("Du bist nicht bei '" + clientTopic + "' subscribed.");
            }
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

    public String[] mySubscribedEvents() {
        String url = "http://localhost:8080/api/clients/" + user.getEmail() + "/subscriptions";
        RestTemplate restTemplate = new RestTemplate();
        try {
            String[] subscribedTopics = restTemplate.getForObject(url, String[].class);
            return (subscribedTopics != null) ? subscribedTopics : new String[0];
        } catch (Exception e) {
            System.out.println("Fehler beim Laden der Subscriptions: " + e.getMessage());
            return new String[0];
        }
    }

    public void listMySubscribedEvents() {
        String[] subscribedTopics = mySubscribedEvents();
        if (subscribedTopics.length == 0) {
            System.out.println("Du bist zu keinem Event subscribed.");
        } else {
            System.out.println("Du bist subscribed zu:");
            for (String topic : subscribedTopics) {
                System.out.println(" - " + topic);
            }
        }
    }


    public String[] listMyUnsubscribedEvents() {
        String url = "http://localhost:8080/api/admin/events";
        RestTemplate restTemplate = new RestTemplate();
        try {
            String[] allEvents = restTemplate.getForObject(url, String[].class);
            if (allEvents == null || allEvents.length == 0) {
                System.out.println("Keine Events gefunden.");
                return new String[0];
            }

            String[] subscribedEvents = mySubscribedEvents();
            List<String> unsubscribed = new ArrayList<>();
            for (String event : allEvents) {
                if (!Arrays.asList(subscribedEvents).contains(event)) {
                    unsubscribed.add(event);
                    System.out.println(" - " + event);
                }
            }

            return unsubscribed.toArray(new String[0]);
        } catch (Exception e) {
            System.out.println("Fehler beim Laden der Events: " + e.getMessage());
            return new String[0];
        }
    }

    public static String formatTimestamp(long timestamp) {
        return Instant.ofEpochMilli(timestamp)
                .atZone(ZoneId.systemDefault())
                .format(DateTimeFormatter.ofPattern("HH:mm"));
    }


    public void disconnect() {
        log.info("Client '{}' trennt Verbindung und meldet sich von allen Topics ab", user.getEmail());
        for (Subscription sub : subscriptions.values()) {
            sub.unsubscribe();
        }
        subscriptions.clear();

        if (session != null && session.isConnected()) {
            session.disconnect();
            log.info("Client '{}' hat die WebSocket-Verbindung getrennt", user.getEmail());
        }
    }
}