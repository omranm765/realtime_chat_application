package de.htwsaar.messagebroker.controller;

import de.htwsaar.messagebroker.repository.ClientRepository;
import de.htwsaar.messagebroker.service.ClientSubscriptionService;
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

    private final ClientSubscriptionService clientSubscriptionService;

    public ClientSubscriptionController(ClientRepository clientRepo,
                                        ClientSubscriptionService clientSubscriptionService) {
        this.clientRepo = clientRepo;
        this.clientSubscriptionService = clientSubscriptionService;
    }

    @PostMapping("/{clientId}/subscribe/{event}")
    public void subscribeClientToEvent(@PathVariable String clientId, @PathVariable String event) {
        clientSubscriptionService.subscribeClientToEvent(clientId, event);
    }

    @DeleteMapping("/{clientId}/unsubscribe/{event}")
    public void unsubscribeClientFromEvent(@PathVariable String clientId, @PathVariable String event) {
        clientSubscriptionService.unsubscribeClientFromEvent(clientId, event);
    }

    @GetMapping("/{clientId}/subscriptions")
    public Set<String> getClientSubscriptions(@PathVariable String clientId) {
        return clientSubscriptionService.getClientSubscriptions(clientId);
    }

}
