package de.htwsaar.messagebroker.service;

import de.htwsaar.messagebroker.controller.UserController;
import de.htwsaar.messagebroker.repository.ClientRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

@Service
public class ClientSubscriptionService {

    private final ClientRepository clientRepo;
    private static final Logger log = LoggerFactory.getLogger(UserController.class);

    public ClientSubscriptionService(ClientRepository clientRepo) {
        this.clientRepo = clientRepo;
    }

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

    public void unsubscribeClientFromEvent(@PathVariable String clientId, @PathVariable String event) {
        clientRepo.findById(clientId).ifPresent(client -> {
            log.info("Client '{}' unsubscribed von '{}'", clientId, event);
            System.out.println("→ Unsubscribing " + clientId + " from " + event);
            client.getSubscribedEventTypes().remove(event);
            clientRepo.save(client);
        });
    }

    public Set<String> getClientSubscriptions(@PathVariable String clientId) {
        log.info("Abfrage der Subscriptions von Client '{}'", clientId);
        return clientRepo.findById(clientId)
                .map(c -> c.getSubscribedEventTypes())
                .orElse(Collections.emptySet());
    }

}
