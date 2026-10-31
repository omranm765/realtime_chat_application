package de.htwsaar.messagebroker.controller;

import de.htwsaar.messagebroker.repository.ClientRepository;
import de.htwsaar.messagebroker.service.HeartbeatService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api")
public class HeartbeatController {

    private final ClientRepository clientRepo;
    private final HeartbeatService heartbeatService;

    public HeartbeatController(ClientRepository clientRepo,
                               HeartbeatService heartbeatService) {
        this.clientRepo = clientRepo;
        this.heartbeatService = heartbeatService;
    }

    @PostMapping("/heartbeat")
    public ResponseEntity<Void> receiveHeartbeat(@RequestBody String email) {
        return heartbeatService.receiveHeartbeat(email);
    }
}
