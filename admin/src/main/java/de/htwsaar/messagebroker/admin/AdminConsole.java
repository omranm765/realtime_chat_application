package de.htwsaar.messagebroker.admin;

import java.util.Scanner;

public class AdminConsole {

    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);
        AdminService service = new AdminService();

        System.out.println("MessageBroker Admin-Client gestartet.");
        System.out.println("Befehle: create-event <name>, delete-event <event>, list-events, list-users, status," +
                " commands, exit");

        while (true) {
            System.out.print("> ");
            String input = scanner.nextLine().trim();
            String[] parts = input.split(" ", 2);

            switch (parts[0]) {
                case "create-event" -> {
                    if (parts.length < 2) {
                        System.out.println("Fehlender Eventname.");
                    } else {
                        service.createEventType(parts[1]);
                    }
                }
                case "list-events" -> service.listEventTypes();
                case "list-users" -> service.listUsers();
                case "status" -> service.status();
                case "delete-event" -> {
                    if (parts.length < 2) {
                        System.out.println("Fehlender Eventname.");
                    } else {
                        service.deleteEventType(parts[1]);
                    }
                }
                case "commands" -> System.out.println("Befehle: create-event <name>, " +
                        "delete-event <event>, list-events, list-users, status, commands, exit");
                case "exit" -> {
                    System.out.println("Admin-Client wird beendet...");
                    return;
                }
                default -> System.out.println("Unbekannter Befehl.");
            }
        }
    }
}
