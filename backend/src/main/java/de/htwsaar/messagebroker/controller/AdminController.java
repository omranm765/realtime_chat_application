package de.htwsaar.messagebroker.controller;

import de.htwsaar.messagebroker.entity.ClientEntity;
import de.htwsaar.messagebroker.repository.ChatMessageRepository;
import de.htwsaar.messagebroker.repository.ClientRepository;
import de.htwsaar.messagebroker.repository.EventTypeRepository;
import jakarta.transaction.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin")
public class AdminController {

    private final EventTypeRepository eventRepo;
    private final ClientRepository clientRepo;
    private final ChatMessageRepository chatMessageRepo;

    private static final Logger log = LoggerFactory.getLogger(AdminController.class);

    public AdminController(EventTypeRepository eventRepo, ClientRepository clientRepo
            , ChatMessageRepository chatMessageRepository) {
        this.eventRepo = eventRepo;
        this.clientRepo = clientRepo;
        this.chatMessageRepo = chatMessageRepository;
    }

    @PostMapping("/events/{name}")
    public void createEvent(@PathVariable("name") String name) {
        if (!eventRepo.findByName(name).isPresent()) {
            var event = new de.htwsaar.messagebroker.entity.EventTypeEntity();
            event.setName(name);
            eventRepo.save(event);
            log.info("Neues Event erstellt: {}", name);
        } else {
            log.info("Event '{}' existiert bereits", name);
        }
    }

    @PostMapping("/clients")
    public void registerClient(@RequestBody String clientId) {
        if (!clientRepo.existsById(clientId)) {
            var entity = new de.htwsaar.messagebroker.entity.ClientEntity();
            entity.setId(clientId);
            clientRepo.save(entity);
            log.info("Neuer Client registriert: {}", clientId);
        } else {
            log.info("Client '{}' ist bereits registriert", clientId);
        }
    }


    @GetMapping("/events")
    public List<String> listEvents() {
        List<String> events = eventRepo.findAll().stream()
                .map(de.htwsaar.messagebroker.entity.EventTypeEntity::getName)
                .toList();
        log.info("Alle Events abgefragt ({} gefunden)", events.size());
        return events;
    }

    @GetMapping("/clients")
    public List<String> listClients() {
        List<String> clients = clientRepo.findAll().stream()
                .map(de.htwsaar.messagebroker.entity.ClientEntity::getId)
                .toList();
        log.info("Alle Clients abgefragt ({} gefunden)", clients.size());
        return clients;
    }

    @GetMapping("/status")
    public String status() {
        long events = eventRepo.count();
        long clients = clientRepo.count();
        String msg = "Events: " + events + ", Clients: " + clients;
        log.info("Status abgefragt: {}", msg);
        return msg;
    }

    @Transactional
    @DeleteMapping("/events/{name}")
    public void deleteEvent(@PathVariable String name) {
        chatMessageRepo.deleteByEventType(name);
        eventRepo.findByName(name).ifPresent(event -> {
            eventRepo.delete(event);
            log.info("Event '{}' und zugehörige Nachrichten gelöscht", name);
        });
    }

    @GetMapping("/clients/online")
    public List<String> getOnlineClients() {
        long now = System.currentTimeMillis();
        return clientRepo.findAll().stream()
                .filter(c -> now - c.getLastHeartbeat() < 10_000)
                .map(ClientEntity::getId)
                .toList();
    }

    @GetMapping("/clients/online-details")
    public List<ClientEntity> getOnlineClientDetails() {
        long now = System.currentTimeMillis();
        return clientRepo.findAll().stream()
                .filter(c -> now - c.getLastHeartbeat() < 10_000)
                .toList();
    }

    @PostMapping("/heartbeat/offline")
    public void markClientOffline(@RequestBody String clientId) {
        clientRepo.findById(clientId).ifPresent(client -> {
            client.setLastHeartbeat(0L); // oder System.currentTimeMillis() - 99999999L
            clientRepo.save(client);
        });
    }

}
