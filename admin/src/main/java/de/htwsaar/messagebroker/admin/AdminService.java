package de.htwsaar.messagebroker.admin;

import de.htwsaar.messagebroker.entity.UserEntity;
import org.springframework.web.client.RestTemplate;
import org.springframework.http.ResponseEntity;

public class AdminService {

    private final String baseUrl = "http://localhost:8080/api/admin";
    private final RestTemplate rest = new RestTemplate();

    public void createEventType(String name) {
        if (name == null || name.trim().isEmpty() || name.contains(" ")) {
            System.out.println("Ungültiger Event-Name. Keine Leerzeichen erlaubt.");
            return;
        }
        rest.postForEntity(baseUrl + "/events/" + name, null, Void.class);
        System.out.println("Event '" + name + "' erstellt.");
    }

    public void listEventTypes() {
        String[] events = rest.getForObject(baseUrl + "/events", String[].class);
        System.out.println("Verfuegbare Event-Typen:");
        for (String e : events) {
            System.out.println(" - " + e);
        }
    }

    public void listUsers() {
        String baseUserUrl = "http://localhost:8080/api/users";
        UserEntity[] users = rest.getForObject(baseUserUrl, UserEntity[].class);
        System.out.println("Registrierte Benutzer:");
        for (UserEntity user : users) {
            System.out.println(" - " + user.getId() + " (" + user.getDisplayName() + ")");
        }
    }

    public void status() {
        String status = rest.getForObject(baseUrl + "/status", String.class);
        System.out.println("Status: " + status);
    }

    public void deleteEventType(String name) {
        rest.delete(baseUrl + "/events/" + name);
        System.out.println("Event '" + name + "' geloescht.");
    }
}
