package de.htwsaar.messagebroker.controller;

import de.htwsaar.messagebroker.entity.ClientEntity;
import de.htwsaar.messagebroker.repository.ChatMessageRepository;
import de.htwsaar.messagebroker.repository.ClientRepository;
import de.htwsaar.messagebroker.repository.EventTypeRepository;
import de.htwsaar.messagebroker.service.AdminService;
import jakarta.transaction.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin")
public class AdminController {

    private final AdminService adminService;
    /*private final EventTypeRepository eventRepo;
    private final ClientRepository clientRepo;
    private final ChatMessageRepository chatMessageRepo;
    */

    private static final Logger log = LoggerFactory.getLogger(AdminController.class);

    public AdminController(EventTypeRepository eventRepo, ClientRepository clientRepo
            , ChatMessageRepository chatMessageRepository, AdminService adminService) {
        //this.eventRepo = eventRepo;
        //this.clientRepo = clientRepo;
        //this.chatMessageRepo = chatMessageRepository;
        this.adminService = adminService;
    }

    @PostMapping("/events/{name}")
    public void createEvent(@PathVariable("name") String name) {
        adminService.createEvent(name);
    }

    @PostMapping("/clients")
    public void registerClient(@RequestBody String clientId) {
        adminService.registerClient(clientId);
    }


    @GetMapping("/events")
    public List<String> listEvents() {
        return adminService.listEvents();
    }

    @GetMapping("/clients")
    public List<String> listClients() {
        return adminService.listClients();
    }

    @GetMapping("/status")
    public String status() {
        return adminService.status();
    }

    @Transactional
    @DeleteMapping("/events/{name}")
    public void deleteEvent(@PathVariable String name) {
        adminService.deleteEvent(name);
    }

    @GetMapping("/clients/online")
    public List<String> getOnlineClients() {
        return adminService.getOnlineClients();
    }

    @GetMapping("/clients/online-details")
    public List<ClientEntity> getOnlineClientDetails() {
        return adminService.getOnlineClientDetails();
    }

    @PostMapping("/heartbeat/offline")
    public void markClientOffline(@RequestBody String clientId) {
        adminService.markClientOffline(clientId);
    }

}
