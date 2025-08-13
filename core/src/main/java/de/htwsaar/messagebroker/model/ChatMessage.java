package de.htwsaar.messagebroker.model;

/**
 * Diese Klasse repräsentiert eine Chat-Nachricht, die zwischen Clients
 * über den Message Broker ausgetauscht wird.
 *
 * Sie enthält alle relevanten Informationen zur Darstellung und
 * zum Nachverfolgen von Nachrichten (z. B. für Offline-Zustellung).
 */
public class ChatMessage {

    /**
     * Eindeutige ID der Nachricht (wird vom Backend vergeben).
     * Notwendig, um Zustellstatus zu speichern (siehe MessageDeliveryEntity).
     */
    private Long id;

    /**
     * ID des Benutzers, der die Nachricht gesendet hat.
     * Wird verwendet, um den Absender zu identifizieren.
     */
    private Long senderId;

    /**
     * Anzeigename des Absenders, dient zur Darstellung im Chat.
     */
    private String senderDisplayName;

    /**
     * Event-Name (z. B. "chatroom1"), gibt an, zu welchem Topic die Nachricht gehört.
     */
    private String eventType;

    /**
     * Der eigentliche Inhalt der Nachricht (Text).
     */
    private String content;

    /**
     * Zeitpunkt der Nachricht (Millisekunden seit Unix-Epoch).
     * Dient zur chronologischen Sortierung und Anzeige.
     */
    private long timestamp;

    // Standard-Konstruktor (z. B. für JSON-Deserialisierung)
    public ChatMessage() {}

    public ChatMessage(Long senderId, String senderDisplayName, String eventType, String content, long timestamp) {
        this.senderId = senderId;
        this.senderDisplayName = senderDisplayName;
        this.eventType = eventType;
        this.content = content;
        this.timestamp = timestamp;
    }

    /**
     * Gibt die Uhrzeit der Nachricht im HH:mm-Format zurück.
     */
    public String getFormattedTime() {
        java.time.format.DateTimeFormatter formatter =
                java.time.format.DateTimeFormatter.ofPattern("HH:mm")
                        .withZone(java.time.ZoneId.systemDefault());

        return formatter.format(java.time.Instant.ofEpochMilli(timestamp));
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
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
