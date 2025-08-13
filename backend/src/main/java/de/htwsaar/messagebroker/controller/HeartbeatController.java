package de.htwsaar.messagebroker.controller;

import de.htwsaar.messagebroker.repository.ClientRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api")
public class HeartbeatController {

    private final ClientRepository clientRepo;

    public HeartbeatController(ClientRepository clientRepo) {
        this.clientRepo = clientRepo;
    }

    @PostMapping("/heartbeat")
    public ResponseEntity<Void> receiveHeartbeat(@RequestBody String email) {
        clientRepo.findById(email).ifPresent(client -> {
            client.setLastHeartbeat(System.currentTimeMillis());
            clientRepo.save(client);
        });
        return ResponseEntity.ok().build();
    }
}
