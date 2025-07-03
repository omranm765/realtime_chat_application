package de.htwsaar.messagebroker.client;

import de.htwsaar.messagebroker.entity.UserEntity;
import de.htwsaar.messagebroker.model.ChatMessage;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.client.HttpClientErrorException.NotFound;

import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

public class ClientConsole {
    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);
        RestTemplate rest = new RestTemplate();
        WebSocketClientEndpoint client;

        System.out.print("E-Mail: ");
        String email = scanner.nextLine().trim();

        UserEntity user;
        String url = "http://localhost:8080/api/users/by-email?email=" + email;

        try {
            // Login
            user = rest.getForObject(url, UserEntity.class);

            System.out.print("Passwort: ");
            String enteredPassword = scanner.nextLine().trim();

            if (!enteredPassword.equals(user.getPassword())) {
                System.out.println("Falsches Passwort.");
                return;
            }

            System.out.println("Willkommen zurueck, " + user.getDisplayName() + "!");
            // Registriere Client, wenn noch nicht vorhanden
            String registerClientUrl = "http://localhost:8080/api/admin/clients";
            try {
                rest.postForObject(registerClientUrl, user.getEmail(), Void.class);
            } catch (Exception e) {
                System.out.println("Client konnte nicht registriert werden: " + e.getMessage());
            }


        } catch (NotFound ex) {
            // Registrierung
            System.out.print("Neuer Nutzer! Bitte gib deinen Namen ein: ");
            String displayName = scanner.nextLine().trim();

            System.out.print("Passwort: ");
            String password = scanner.nextLine().trim();

            System.out.print("Passwort bestaetigen: ");
            String confirmPassword = scanner.nextLine().trim();

            if (!password.equals(confirmPassword)) {
                System.out.println("Passwoerter stimmen nicht ueberein.");
                return;
            }

            user = new UserEntity();
            user.setDisplayName(displayName);
            user.setEmail(email);
            user.setPassword(password);
            user.setCreatedAt(System.currentTimeMillis());

            try {
                user = rest.postForObject("http://localhost:8080/api/users", user, UserEntity.class);
                System.out.println("Registrierung erfolgreich. Willkommen, " + user.getDisplayName() + "!");
            } catch (Exception e) {
                System.out.println("Registrierung fehlgeschlagen: " + e.getMessage());
                return;
            }

        } catch (Exception e) {
            System.out.println("Fehler beim Zugriff auf den Server: " + e.getMessage());
            return;
        }

        client = new WebSocketClientEndpoint("ws://localhost:8080/ws", user);

        System.out.println("MessageBroker CLI Client gestartet");
        System.out.println("Befehle: subscribe <event>, publish <event> <message>," +
                " multi-publish <event>, unsubscribe, list-events," +
                " my-events, commands, exit");

        while (true) {
            System.out.print("> ");
            String input = scanner.nextLine().trim();
            String[] parts = input.split(" ", 3);

            if (parts[0].equalsIgnoreCase("exit")) {
                System.out.println("Beende Client...");
                client.disconnect();
                break;
            } else if (parts[0].equalsIgnoreCase("subscribe") && parts.length >= 2) {
                client.subscribe(parts[1]);
            } else if (parts[0].equalsIgnoreCase("publish") && parts.length == 3) {
                client.publish(parts[1], parts[2]);
            } else if (parts[0].equalsIgnoreCase("unsubscribe")) {
                client.unsubscribe();
            } else if (parts[0].equalsIgnoreCase("list-events")) {
                client.listEventTypes();
            } else if (parts[0].equalsIgnoreCase("my-events")) {
                client.listMySubscribedEvents();
            } else if (parts[0].equalsIgnoreCase("broadcast") && parts.length >= 2) {
                client.multi_publish(parts[1]);
            } else if (parts[0].equalsIgnoreCase("commands")) {
                System.out.println("Befehle: subscribe <event>, publish <event> <message>," +
                        ", multi-publish <event>, unsubscribe, list-events, my-events, exit");
            } else {
                System.out.println("Ungueltiger Befehl. Benutze: subscribe <event>, publish <event> <message>, unsubscribe, list-events, my-events, exit");
            }
        }
    }
}
