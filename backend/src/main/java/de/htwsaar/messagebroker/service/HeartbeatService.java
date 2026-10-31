package de.htwsaar.messagebroker.service;

import de.htwsaar.messagebroker.repository.ClientRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.RequestBody;

@Service
public class HeartbeatService {

    private final ClientRepository clientRepo;

    public HeartbeatService(ClientRepository clientRepo) {
        this.clientRepo = clientRepo;
    }

    public ResponseEntity<Void> receiveHeartbeat(@RequestBody String email) {
        clientRepo.findById(email).ifPresent(client -> {
            client.setLastHeartbeat(System.currentTimeMillis());
            clientRepo.save(client);
        });
        return ResponseEntity.ok().build();
    }
}
