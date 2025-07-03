package de.htwsaar.messagebroker.model;

public class EventType {
    private String name;

    public EventType() {}

    public EventType(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
}
