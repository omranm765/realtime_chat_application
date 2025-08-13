package de.htwsaar.messagebroker.controller;

import de.htwsaar.messagebroker.repository.ClientRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.*;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

@RestController
@RequestMapping("/api/clients")
public class ClientSubscriptionController {

    private final ClientRepository clientRepo;
    private static final Logger log = LoggerFactory.getLogger(UserController.class);

    public ClientSubscriptionController(ClientRepository clientRepo) {
        this.clientRepo = clientRepo;
    }

    @PostMapping("/{clientId}/subscribe/{event}")
    public void subscribeClientToEvent(@PathVariable String clientId, @PathVariable String event) {
        clientRepo.findById(clientId).ifPresent(client -> {
            log.info("Client '{}' subscribed zu '{}'", clientId, event);
            System.out.println("→ Subscribing " + clientId + " to " + event);
            Set<String> updated = new HashSet<>(client.getSubscribedEventTypes());
            updated.add(event);
            client.setSubscribedEventTypes(updated);

            clientRepo.save(client);
        });
    }

    @DeleteMapping("/{clientId}/unsubscribe/{event}")
    public void unsubscribeClientFromEvent(@PathVariable String clientId, @PathVariable String event) {
        clientRepo.findById(clientId).ifPresent(client -> {
            log.info("Client '{}' unsubscribed von '{}'", clientId, event);
            System.out.println("→ Unsubscribing " + clientId + " from " + event);
            client.getSubscribedEventTypes().remove(event);
            clientRepo.save(client);
        });
    }

    @GetMapping("/{clientId}/subscriptions")
    public Set<String> getClientSubscriptions(@PathVariable String clientId) {
        log.info("Abfrage der Subscriptions von Client '{}'", clientId);
        return clientRepo.findById(clientId)
                .map(c -> c.getSubscribedEventTypes())
                .orElse(Collections.emptySet());
    }

}
