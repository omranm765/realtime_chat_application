package de.htwsaar.messagebroker.controller;

import de.htwsaar.messagebroker.repository.ChatMessageRepository;
import de.htwsaar.messagebroker.repository.ClientRepository;
import de.htwsaar.messagebroker.repository.EventTypeRepository;
import jakarta.transaction.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin")
public class AdminController {

    private final EventTypeRepository eventRepo;
    private final ClientRepository clientRepo;
    private final ChatMessageRepository chatMessageRepo;

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
        }
    }

    @PostMapping("/clients")
    public void registerClient(@RequestBody String clientId) {
        if (!clientRepo.existsById(clientId)) {
            var entity = new de.htwsaar.messagebroker.entity.ClientEntity();
            entity.setId(clientId);
            clientRepo.save(entity);
        }
    }


    @GetMapping("/events")
    public List<String> listEvents() {
        return eventRepo.findAll().stream()
                .map(de.htwsaar.messagebroker.entity.EventTypeEntity::getName)
                .toList();
    }

    @GetMapping("/clients")
    public List<String> listClients() {
        return clientRepo.findAll().stream()
                .map(de.htwsaar.messagebroker.entity.ClientEntity::getId)
                .toList();
    }

    @GetMapping("/status")
    public String status() {
        long events = eventRepo.count();
        long clients = clientRepo.count();
        return "Events: " + events + ", Clients: " + clients;
    }

    @Transactional
    @DeleteMapping("/events/{name}")
    public void deleteEvent(@PathVariable String name) {
        chatMessageRepo.deleteByEventType(name);
        eventRepo.findByName(name).ifPresent(eventRepo::delete);
    }
}
