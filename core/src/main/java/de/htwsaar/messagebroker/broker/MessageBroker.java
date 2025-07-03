package de.htwsaar.messagebroker.broker;

import de.htwsaar.messagebroker.model.ChatMessage;
import de.htwsaar.messagebroker.model.Client;

import java.util.*;

public class MessageBroker {
    private final Map<String, Client> clients = new HashMap<>();
    private final Map<String, List<ChatMessage>> messageQueue = new HashMap<>();
    private final Set<String> eventTypes = new HashSet<>();

    public void registerClient(String clientId) {
        clients.putIfAbsent(clientId, new Client(clientId));
    }

    public void defineEventType(String eventType) {
        eventTypes.add(eventType);
    }

    public void subscribe(String clientId, String eventType) {
        Client client = clients.get(clientId);
        if (client != null && eventTypes.contains(eventType)) {
            client.subscribe(eventType);
        }
    }

    public void unsubscribe(String clientId, String eventType) {
        Client client = clients.get(clientId);
        if (client != null) {
            client.unsubscribe(eventType);
        }
    }

    public void publish(ChatMessage message) {
        if (!eventTypes.contains(message.getEventType())) return;
        messageQueue.computeIfAbsent(message.getEventType(), k -> new ArrayList<>()).add(message);
    }

    public List<ChatMessage> retrieveMessages(String clientId) {
        Client client = clients.get(clientId);
        if (client == null) return Collections.emptyList();
        List<ChatMessage> result = new ArrayList<>();
        for (String eventType : client.getSubscribedEventTypes()) {
            List<ChatMessage> messages = messageQueue.getOrDefault(eventType, new ArrayList<>());
            result.addAll(messages);
        }
        return result;
    }

    public Set<String> getDefinedEventTypes() {
        return eventTypes;
    }

    public boolean isClientRegistered(String clientId) {
        return clients.containsKey(clientId);
    }
}
