# MessageBroker – Echtzeit-Chat mit WebSocket & REST

Ein Java-basiertes, modular aufgebautes Echtzeit-Chat-System mit einem selbst entwickelten Message Broker, WebSocket-Kommunikation (STOMP), REST-Schnittstellen, Admin-Konsole und CLI-Client.

Ziel ist es, Publisher-Subscriber-Kommunikation für Chat-Nachrichten über Events zu ermöglichen – synchron (REST) und asynchron (WebSocket). Die Nachrichten sollen persistent gespeichert und offline zugestellt werden können. Der Admin kann Eventtypen verwalten und Systemstatus abrufen.

---

## Architektur

### Lösungsstrategie

Die Anwendung basiert auf einer Microservice-Architektur mit Spring Boot und verwendet STOMP über WebSocket für asynchrone Kommunikation. REST wird für synchrone Registrierungen, Verwaltung und Nachrichtenverfolgung verwendet.

Das System besteht aus mehreren Modulen:

* `backend`: zentrale REST-API und WebSocket-Endpunkt
* `client`: CLI-Nutzeranwendung (subscribe, publish, etc.)
* `admin`: CLI-Adminanwendung (Events, Clients, Status)
* `database`: Persistenzschicht mit JPA/MySQL

### Use Cases

* Nutzer kann sich registrieren, einloggen und Events abonnieren
* Nutzer kann Nachrichten senden und empfangen
* Nachrichten können auch empfangen werden, wenn der Nutzer offline war
* Admin kann Events definieren/löschen, Online-Clients anzeigen, Systemstatus abfragen

### Anforderungen

| Anforderung                   | Erfüllung | Erläuterung                                                                     |
| ----------------------------- | --------- | ------------------------------------------------------------------------------- |
| **MUST Have Anforderungen**   |           |                                                                                 |
| M1                            | ja        | STOMP/WebSocket zwischen Publisher & Subscriber mit Spring Boot umgesetzt       |
| M2                            | ja        | REST-API für Registrierung, Publish/Subscribe vorhanden                         |
| M3                            | ja        | CLI-Client erlaubt publish, subscribe, unsubscribe, etc.                        |
| M4                            | ja        | Admin-CLI mit Eventverwaltung & Statusabruf realisiert                          |
| M5                            | ja        | Events sind dynamisch definierbar via Admin-CLI                                 |
| M6                            | ja        | Sowohl Nutzer- als auch Admin-CLI umgesetzt                                     |
| **SHOULD Have Anforderungen** |           |                                                                                 |
| S1                            | ja        | Parallelverarbeitung durch Spring Boot, Logger zeigt Thread-IDs an              |
| S2                            | ja        | Persistenz durch JPA + MySQL für Nutzer, Nachrichten, Events, Zustellungsstatus |
| S3                            | ja        | REST-Endpunkt `/api/admin/status` zeigt Event- & Clientanzahl                   |
| S4                            | ja        | Beispiel-Client (ClientConsole.java) mit allen Funktionen vorhanden             |
| S5                            | ja        | Offline-Zustellung durch Nachrichtenabfrage + Delivered-Flag                    |
| **COULD Have Anforderungen**  |           |                                                                                 |
| C1                            | nein      | Kein vernetzter Broker, keine horizontale Skalierung                            |
| C2                            | ja        | Nachrichtenzustellung wird erkannt (delivered-Flag in DB)                       |
| C3                            | ja        | Logging in REST- und WS-Komponenten mit Slf4j (Infos, Fehler)                   |
| C4                            | ja        | Admin-Interface erlaubt Event- und Systemverwaltung über CLI                    |
| C5                            | ja        | Heartbeat + Online-Status-Management über REST + Timestamp-Check                |
| **++ Anforderungen**          |           |                                                                                 |
| ++1                           | ja        | Asynchrone Kommunikation über WebSocket mit STOMP                               |
| ++2                           | ja        | Drei-Schichten-Architektur (Backend, DB, Clients) realisiert                    |
| ++3                           | nein      | Keine Broker-Vernetzung / kein skalierbares Netzwerk                            |

---

## Statisches Modell

### Komponentendiagramm

* `backend`: REST-API, STOMP-Endpoint, Controller
* `client`: CLI-Client für Nutzer mit `WebSocketClientEndpoint`
* `admin`: CLI für Eventverwaltung
* `database`: `UserEntity`, `ChatMessageEntity`, `MessageDeliveryEntity`, `ClientEntity`, `EventTypeEntity`

### Verteilungsdiagramm

* Client und Admin CLI laufen lokal
* Backend (Spring Boot) läuft als Server-Prozess
* MySQL-Datenbank läuft als separater Dienst

### Klassendiagramme

* `UserEntity`: ID, Email, Passwort, DisplayName
* `ChatMessageEntity`: ID, Content, Timestamp, SenderId, EventType
* `MessageDeliveryEntity`: clientId, messageId, deliveredAt
* `ClientEntity`: clientId, subscribedEvents, lastHeartbeat

### API (REST)

* `POST /api/users` – Registrierung
* `GET /api/users/by-email?email=` – Login-Validierung
* `POST /api/admin/events` – Event definieren
* `GET /api/admin/status` – Status anzeigen
* `POST /api/messages/mark-delivered` – Nachrichten als gelesen markieren
* `GET /api/messages/undelivered?clientId=` – verpasste Nachrichten abfragen
* `POST /api/heartbeat` – Client-Online-Heartbeat
* `POST /api/heartbeat/offline` – Client explizit als offline setzen
* `GET /api/admin/clients/online` – Online-Clients anzeigen

### Datenbankmodell

* 4 Tabellen: `users`, `chat_messages`, `message_delivery`, `clients`
* Relationen zwischen Nachrichten, Nutzern und Delivery-Status

---

## Dynamisches Modell

### Aktivittsdiagramm

* Registrierung/Login
* Nachricht senden
* Nachricht empfangen
* Offline-Zustellung mit „Delivered“-Status

### Sequenzdiagramm

* Nutzer sendet Nachricht → Server speichert + verteilt → andere Clients empfangen
* Beim Start: verpasste Nachrichten → abgerufen → markiert als geliefert

---

## Getting Started

### Voraussetzungen

* Java 17+
* Maven
* MySQL-Server (Schema: `messagebroker`)

### Installation & Deployment

```bash
# Backend bauen & starten
cd backend
mvn spring-boot:run

# Client starten
cd client
mvn compile
java -cp target/client-1.0.jar de.htwsaar.messagebroker.client.ClientConsole

# Admin starten
cd admin
mvn compile
java -cp target/admin-1.0.jar de.htwsaar.messagebroker.admin.AdminConsole
```

---

## Built With

* **Spring Boot** – REST & WebSocket
* **STOMP** – WebSocket-Protokoll
* **MySQL** – Persistenz
* **JPA/Hibernate** – ORM
* **Maven** – Build Tool
* **Slf4j** – Logging

---

## License

Open Source für akademische Zwecke (MIT oder ähnlich)

## Acknowledgments

* Prof. Dr.-Ing. Klaus Berberich für die Aufgabenstellung
* Inspiration durch Messaging-Systeme wie Kafka & RabbitMQ
