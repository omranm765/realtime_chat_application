package de.htwsaar.messagebroker.client;

import de.htwsaar.messagebroker.entity.UserEntity;
import de.htwsaar.messagebroker.model.ChatMessage;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.client.HttpClientErrorException.NotFound;

import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;
import java.util.Timer;

public class ClientConsole {

    private static Timer heartbeatTimer = new Timer();

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

            user = rest.postForObject("http://localhost:8080/api/users", user, UserEntity.class);

            String registerClientUrl = "http://localhost:8080/api/admin/clients";
            try {
                rest.postForObject(registerClientUrl, user.getEmail(), Void.class);
            } catch (Exception e) {
                System.out.println("Client konnte nicht registriert werden: " + e.getMessage());
            }

            System.out.println("Registrierung erfolgreich. Willkommen, " + user.getDisplayName() + "!");

        } catch (Exception e) {
            System.out.println("Fehler beim Zugriff auf den Server: " + e.getMessage());
            return;
        }

        final String clientEmail = user.getEmail();

        heartbeatTimer.scheduleAtFixedRate(new java.util.TimerTask() {
            @Override
            public void run() {
                try {
                    rest.postForObject("http://localhost:8080/api/heartbeat", clientEmail, Void.class);
                } catch (Exception e) {
                    System.out.println("⚠Heartbeat fehlgeschlagen: " + e.getMessage());
                }
            }
        }, 0, 5000);

        client = new WebSocketClientEndpoint("ws://localhost:8080/ws", user);
        fetchUndeliveredMessages(user.getEmail(), rest);

        System.out.println("MessageBroker CLI Client gestartet");
        printCommands();

        while (true) {
            System.out.print("> ");
            String input = scanner.nextLine().trim();
            String[] parts = input.split(" ", 3);

            if (parts[0].equalsIgnoreCase("exit")) {
                System.out.println("Beende Client...");
                heartbeatTimer.cancel();
                client.disconnect();
                break;
            } else if (parts[0].equalsIgnoreCase("subscribe") && parts.length >= 2) {
                client.subscribe(parts[1]);
            } else if (parts[0].equalsIgnoreCase("publish") && parts.length == 3) {
                client.publish(parts[1], parts[2]);
                printCommands();
            } else if (parts[0].equalsIgnoreCase("unsubscribe")) {
                client.unsubscribe();
                printCommands();
            } else if (parts[0].equalsIgnoreCase("list-events")) {
                client.listEventTypes();
            } else if (parts[0].equalsIgnoreCase("my-events")) {
                client.listMySubscribedEvents();
            } else if (parts[0].equalsIgnoreCase("multi-publish") && parts.length >= 2) {
                client.multi_publish(parts[1]);
                printCommands();
            } else if (parts[0].equalsIgnoreCase("multi-subscribe")) {
                client.multi_subscribe();
                printCommands();
            } else if (parts[0].equalsIgnoreCase("multi-unsubscribe")) {
                client.multi_unsubscribe();
                printCommands();
            } else if (parts[0].equalsIgnoreCase("commands")) {
                printCommands();
            } else if (parts[0].equalsIgnoreCase("online-clients")) {
                fetchOnlineClients(rest);
            } else {
                printCommands();
            }
        }
    }

    private static void fetchOnlineClients(RestTemplate rest) {
        String url = "http://localhost:8080/api/admin/clients/online";
        try {
            String[] online = rest.getForObject(url, String[].class);
            System.out.println("Online-Clients:");
            if (online == null || online.length == 0) {
                System.out.println(" - (keiner online)");
            } else {
                for (String c : online) {
                    System.out.println(" - " + c);
                }
            }
        } catch (Exception e) {
            System.out.println("Fehler beim Abrufen der Online-Clients: " + e.getMessage());
        }
    }


    private static void printCommands() {
        System.out.println("Befehle: subscribe <event>, publish <event> <message>," +
                " multi-publish <event>, unsubscribe," +
                " multi-subscribe, multi-unsubscribe, list-events," +
                " my-events, online-clients, commands, exit");
    }

    private static void fetchUndeliveredMessages(String clientId, RestTemplate rest) {
        String url = "http://localhost:8080/api/messages/undelivered?clientId=" + clientId;
        try {
            ChatMessage[] undelivered = rest.getForObject(url, ChatMessage[].class);

            if (undelivered != null && undelivered.length > 0) {
                System.out.println("Verpasste Nachrichten:");
                for (ChatMessage msg : undelivered) {
                    System.out.printf("[%s] [%s] %s: %s\n",
                            msg.getEventType(),
                            msg.getFormattedTime(),
                            msg.getSenderDisplayName(),
                            msg.getContent());

                    rest.postForObject(
                            "http://localhost:8080/api/messages/mark-delivered?clientId=" + clientId,
                            List.of(msg.getId()),
                            Void.class
                    );


                }
            }
        } catch (Exception e) {
            System.out.println("Fehler beim Abrufen verpasster Nachrichten: " + e.getMessage());
        }
    }

    static class DeliveryPayload {
        public String clientId;
        public Long messageId;

        public DeliveryPayload(String clientId, Long messageId) {
            this.clientId = clientId;
            this.messageId = messageId;
        }
    }

}