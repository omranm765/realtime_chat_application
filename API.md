# API Documentation – MessageBroker

## User

**Create User**
----
Registriert einen neuen Benutzer.

* **URL**

  `/api/users`

* **Method:**  
  `POST`

* **Data Params**
```json
{
  "displayName": "Alice",
  "email": "alice@example.com",
  "password": "secret"
}
```

* **Success Response:**
  * **Code:** 200  
    **Content:**
```json
{
  "id": 1,
  "displayName": "Alice",
  "email": "alice@example.com",
  "createdAt": 1714032929
}
```

* **Error Response:**
  * **Code:** 422 UNPROCESSABLE ENTRY  
    **Content:** `{ "error": "Email bereits registriert" }`

---

**Get User by Email**
----
Lädt einen Benutzer anhand seiner E-Mail.

* **URL**

  `/api/users/by-email?email={email}`

* **Method:**  
  `GET`

* **URL Params**  
  **Required:**  
  `email=[string]`

* **Success Response:**
  * **Code:** 200  
    **Content:** `{ "id": 1, "displayName": "Alice", "email": "alice@example.com" }`

* **Error Response:**
  * **Code:** 404 NOT FOUND  
    **Content:** `{ "error": "User not found" }`

---

## Admin

**Create Event**
----
Erstellt ein neues Event.

* **URL**

  `/api/admin/events/{name}`

* **Method:**  
  `POST`

* **URL Params**  
  **Required:**  
  `name=[string]`

* **Success Response:**
  * **Code:** 200  
    **Content:** `Event 'chatroom1' erstellt.`

* **Error Response:**
  * **Code:** 409 CONFLICT  
    **Content:** `{ "error": "Event existiert bereits" }`

---

**Delete Event**
----
Löscht ein Event und alle zugehörigen Nachrichten.

* **URL**

  `/api/admin/events/{name}`

* **Method:**  
  `DELETE`

* **URL Params**  
  **Required:**  
  `name=[string]`

* **Success Response:**
  * **Code:** 200  
    **Content:** `Event 'chatroom1' gelöscht.`

---

**List Events**
----
Lädt alle verfügbaren Events.

* **URL**

  `/api/admin/events`

* **Method:**  
  `GET`

* **Success Response:**
  * **Code:** 200  
    **Content:** `[ "chatroom1", "chatroom2" ]`

---

**Status**
----
Zeigt Gesamtanzahl der Events & Clients.

* **URL**

  `/api/admin/status`

* **Method:**  
  `GET`

* **Success Response:**
  * **Code:** 200  
    **Content:** `"Events: 3, Clients: 7"`

---

**Register Client**
----
Registriert einen neuen Client anhand seiner E-Mail.

* **URL**

  `/api/admin/clients`

* **Method:**  
  `POST`

* **Data Params**
```json
"alice@example.com"
```

* **Success Response:**
  * **Code:** 200  
    **Content:** `Client registriert.`

---

**List Clients**
----
Zeigt alle registrierten Clients.

* **URL**

  `/api/admin/clients`

* **Method:**  
  `GET`

* **Success Response:**
  * **Code:** 200  
    **Content:** `[ "alice@example.com", "bob@example.com" ]`

---

**Online Clients**
----
Zeigt alle aktiven Clients (Heartbeat < 10s).

* **URL**

  `/api/admin/clients/online`

* **Method:**  
  `GET`

* **Success Response:**
  * **Code:** 200  
    **Content:** `[ "alice@example.com" ]`

---

## Clients & Messages

**Subscribe Client**
----
Abonniert ein Event.

* **URL**

  `/api/clients/{clientId}/subscribe/{event}`

* **Method:**  
  `POST`

* **URL Params**  
  **Required:**  
  `clientId=[string]`  
  `event=[string]`

---

**Unsubscribe Client**
----
Entfernt ein Abo.

* **URL**

  `/api/clients/{clientId}/unsubscribe/{event}`

* **Method:**  
  `DELETE`

---

**List Client Subscriptions**
----
Zeigt alle Subscriptions eines Clients.

* **URL**

  `/api/clients/{clientId}/subscriptions`

* **Method:**  
  `GET`

* **Success Response:**
  * **Code:** 200  
    **Content:** `[ "chatroom1" ]`

---

**Get Messages by Event**
----
Lädt Nachrichten-Historie eines Events.

* **URL**

  `/api/messages/{event}`

* **Method:**  
  `GET`

* **Success Response:**
```json
[
  {
    "id": 1,
    "senderId": 2,
    "senderDisplayName": "Bob",
    "eventType": "chatroom1",
    "content": "Hallo!",
    "timestamp": 1714032929
  }
]
```

---

**Get Undelivered Messages**
----
Lädt Nachrichten, die ein Client noch nicht erhalten hat.

* **URL**

  `/api/messages/undelivered?clientId={id}`

* **Method:**  
  `GET`

* **URL Params**  
  **Required:**  
  `clientId=[string]`

---

**Mark Delivered**
----
Markiert Nachrichten als zugestellt.

* **URL**

  `/api/messages/mark-delivered?clientId={id}`

* **Method:**  
  `POST`

* **Data Params**
```json
[ 1, 2, 3 ]
```

---

## Heartbeat

**Send Heartbeat**
----
Aktualisiert den Online-Status eines Clients.

* **URL**

  `/api/heartbeat`

* **Method:**  
  `POST`

* **Data Params**
```json
"alice@example.com"
```

---

**Mark Client Offline**
----
Setzt einen Client explizit auf offline.

* **URL**

  `/api/admin/heartbeat/offline`

* **Method:**  
  `POST`

* **Data Params**
```json
"alice@example.com"
```
