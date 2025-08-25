# MessageBroker – Echtzeit-Chat mit WebSocket & REST

Ein Java-basiertes, modular aufgebautes Echtzeit-Chat-System mit einem selbst entwickelten Message Broker.  
Das System unterstützt **WebSocket-Kommunikation (STOMP)** für asynchrone Nachrichtenübertragung und **REST-Schnittstellen** für Registrierung, Verwaltung und Statusabfragen.  
Nachrichten werden persistent gespeichert und können bei Offline-Clients nachträglich zugestellt werden.  

---

## Architektur

### Lösungsstrategie

Die Anwendung basiert auf einer **mehrschichtigen Architektur** mit Spring Boot und JPA.  
- **Asynchron:** Nachrichtenübertragung per WebSocket/STOMP  
- **Synchron:** Verwaltung (Registrierung, Events, Status) über REST  
- **Persistenz:** Speicherung von Benutzern, Nachrichten, Events und Zustellstatus in MySQL  

### Modulübersicht

- **core** → Gemeinsame Modellklassen (z. B. `ChatMessage`)  
- **database** → Entities & Repositories (JPA/MySQL)  
- **backend** → REST-API & WebSocket-Server (Spring Boot)  
- **client** → CLI-Client für Benutzer (Subscribe, Publish, History, Offline-Zustellung)  
- **admin** → CLI-Admin-Konsole (Eventverwaltung, Status, User-Listing)  

---

## Use Cases

- Nutzer kann sich **registrieren** oder einloggen  
- Nutzer kann **Events abonnieren** und Nachrichten senden/empfangen  
- Verpasste Nachrichten werden bei Reconnect **nachträglich zugestellt**  
- Admin kann **Events anlegen/löschen**, **Clients registrieren**, **Systemstatus** abrufen  
- Online-Clients werden über Heartbeats verwaltet  

---

## Anforderungen

| Anforderung                   | Erfüllung | Erläuterung                                                                 |
| ----------------------------- | --------- | --------------------------------------------------------------------------- |
| **MUST Have**                 |           |                                                                             |
| M1                            | ✅        | STOMP/WebSocket zwischen Publisher & Subscriber umgesetzt                   |
| M2                            | ✅        | REST-API für Registrierung, Publish/Subscribe vorhanden                     |
| M3                            | ✅        | CLI-Client erlaubt publish, subscribe, unsubscribe etc.                     |
| M4                            | ✅        | Admin-CLI mit Eventverwaltung & Statusabfrage realisiert                    |
| M5                            | ✅        | Events sind dynamisch definierbar via Admin-CLI                             |
| M6                            | ✅        | Sowohl Nutzer- als auch Admin-CLI umgesetzt                                 |
| **SHOULD Have**               |           |                                                                             |
| S1                            | ✅        | Parallelverarbeitung durch Spring Boot (Thread-Logging)                     |
| S2                            | ✅        | Persistenz über JPA + MySQL für User, Nachrichten, Events, Delivery-Status  |
| S3                            | ✅        | `/api/admin/status` liefert Events- & Client-Anzahl                         |
| S4                            | ✅        | Beispiel-Client (`ClientConsole`) mit allen Funktionen vorhanden            |
| S5                            | ✅        | Offline-Zustellung mit Delivered-Flag realisiert                            |
| **COULD Have**                |           |                                                                             |
| C1                            | ❌        | Keine horizontale Skalierung / Broker-Vernetzung                            |
| C2                            | ✅        | Zustellstatus wird in DB mit Delivered-Flag gespeichert                     |
| C3                            | ✅        | Logging mit Slf4j in REST- und WS-Komponenten                               |
| C4                            | ✅        | Admin-CLI erlaubt Event- und Systemverwaltung                               |
| C5                            | ✅        | Heartbeat + Online-Status-Management per REST & Timestamp                   |
| **++ Anforderungen**          |           |                                                                             |
| ++1                           | ✅        | Asynchrone Kommunikation via STOMP-WebSocket                                |
| ++2                           | ✅        | Drei-Schichten-Architektur (Backend, DB, Clients)                           |
| ++3                           | ❌        | Keine Broker-Vernetzung / Cluster-Modus                                     |

---

## Statisches Modell

### Komponenten

- **backend**  
  - `UserController`, `ChatMessageController`, `AdminController`, `ClientSubscriptionController`, `HeartbeatController`  
  - `WebSocketConfig`, `ChatController` (STOMP)  

- **client**  
  - `ClientConsole` (CLI)  
  - `WebSocketClientEndpoint`, `MessageHandler`  

- **admin**  
  - `AdminConsole` (CLI)  
  - `AdminService`  

- **database**  
  - Entities: `UserEntity`, `ChatMessageEntity`, `MessageDeliveryEntity`, `ClientEntity`, `EventTypeEntity`  
  - Repositories: `UserRepository`, `ChatMessageRepository`, `MessageDeliveryRepository`, `ClientRepository`, `EventTypeRepository`  

### Datenbankmodell

- **users** → Benutzer (id, email, displayName, password, createdAt)  
- **chat_messages** → Nachrichten (id, senderId, eventType, content, timestamp)  
- **message_delivery** → Zustellungsstatus (id, messageId, clientId, deliveredAt)  
- **event_types** → Event-Typen (id, name)  
- **clients** → Clients (id, lastHeartbeat)  
- **client_subscriptions** → Zwischentabelle (clientId, eventType)  

---

## REST API

### User
- `POST /api/users` – Registrierung  
- `GET /api/users/by-email?email=` – Login  
- `GET /api/users/{id}` – User nach ID  

### Events & Admin
- `POST /api/admin/events/{name}` – Event anlegen  
- `DELETE /api/admin/events/{name}` – Event löschen  
- `GET /api/admin/events` – Alle Events  
- `GET /api/admin/status` – Status (Anzahl Events & Clients)  
- `POST /api/admin/clients` – Client registrieren  
- `GET /api/admin/clients` – Alle Clients  
- `GET /api/admin/clients/online` – Online-Clients  
- `GET /api/admin/clients/online-details` – Details zu Online-Clients  
- `POST /api/admin/heartbeat/offline` – Client explizit offline setzen  

### Clients & Messages
- `POST /api/clients/{clientId}/subscribe/{event}` – Event abonnieren  
- `DELETE /api/clients/{clientId}/unsubscribe/{event}` – Abo entfernen  
- `GET /api/clients/{clientId}/subscriptions` – Aktive Subscriptions  

- `GET /api/messages/{event}` – Nachrichten-Historie  
- `GET /api/messages/undelivered?clientId=` – Ungestellte Nachrichten  
- `POST /api/messages/mark-delivered?clientId=` – Nachrichten als zugestellt markieren  

### Heartbeat
- `POST /api/heartbeat` – Client hält sich online  

---

## Dynamisches Modell

- **Registrierung/Login**: User wird erstellt oder aus DB geladen  
- **Subscribe**: Client abonniert Topic → DB & WS-Subscription  
- **Publish**: Nachricht gespeichert → an Subscribed Clients verteilt → Delivery markiert  
- **Offline-Zustellung**: Beim Reconnect → ungelesene Nachrichten werden nachgeladen  
- **Admin**: Kann Events verwalten, Status abrufen, Clients sehen  

---

## Getting Started

### Voraussetzungen
- Java 17+  
- Maven 3.9+  
- MySQL (Schema: `messagebroker_db`)  

### Installation

```bash
# Komplettes Projekt bauen
mvn clean install -U
```

### Starten

```bash
# Backend starten (REST + WebSocket)
cd realtime_chat_application-main
cd backend
mvn spring-boot:run

# Admin-CLI starten
cd realtime_chat_application-main
cd admin
mvn compile
mvn exec:java

# Client-CLI starten/multiple instances allowed
cd realtime_chat_application-main
cd client
mvn compile
mvn exec:java
```

---

## CLI-Befehle

### AdminConsole
```
create-event <name>
delete-event <name>
list-events
list-users
status
commands
exit
```

### ClientConsole
```
subscribe <event>
publish <event> <message>
unsubscribe
multi-subscribe
multi-unsubscribe
multi-publish <event>
list-events
my-events
online-clients
commands
exit
```

---

## Built With
- **Spring Boot** – REST & WebSocket  
- **STOMP** – WebSocket-Protokoll  
- **MySQL** – Persistenz  
- **JPA/Hibernate** – ORM  
- **Maven** – Build Tool  
- **Slf4j** – Logging  

---

## License
Open Source für akademische Zwecke (MIT oder ähnlich)  

