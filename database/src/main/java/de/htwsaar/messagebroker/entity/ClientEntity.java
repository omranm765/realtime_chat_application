package de.htwsaar.messagebroker.entity;

import jakarta.persistence.*;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "clients")
public class ClientEntity {

    @Id
    private String id;

    @ElementCollection
    @CollectionTable(name = "client_subscriptions", joinColumns = @JoinColumn(name = "client_id"))
    @Column(name = "event_type")
    private Set<String> subscribedEventTypes = new HashSet<>();

    private long lastHeartbeat;

    public ClientEntity() {}

    public ClientEntity(String id) {
        this.id = id;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public Set<String> getSubscribedEventTypes() {
        return subscribedEventTypes;
    }

    public void setSubscribedEventTypes(Set<String> subscribedEventTypes) {
        this.subscribedEventTypes = subscribedEventTypes;
    }

    public long getLastHeartbeat() {
        return lastHeartbeat;
    }

    public void setLastHeartbeat(long lastHeartbeat) {
        this.lastHeartbeat = lastHeartbeat;
    }
}
