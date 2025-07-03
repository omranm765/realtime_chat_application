package de.htwsaar.messagebroker.model;

import java.util.HashSet;
import java.util.Set;

public class Client {
    private String id;
    private Set<String> subscribedEventTypes = new HashSet<>();

    public Client() {}

    public Client(String id) {
        this.id = id;
    }

    public String getId() {
        return id;
    }

    public void subscribe(String eventType) {
        subscribedEventTypes.add(eventType);
    }

    public void unsubscribe(String eventType) {
        subscribedEventTypes.remove(eventType);
    }

    public Set<String> getSubscribedEventTypes() {
        return subscribedEventTypes;
    }
}
