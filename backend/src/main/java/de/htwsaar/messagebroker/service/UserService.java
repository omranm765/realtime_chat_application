package de.htwsaar.messagebroker.service;

import de.htwsaar.messagebroker.controller.UserController;
import de.htwsaar.messagebroker.entity.UserEntity;
import de.htwsaar.messagebroker.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

@Service
public class UserService {

    private final UserRepository repo;
    private static final Logger log = LoggerFactory.getLogger(UserController.class);

    public UserService(UserRepository repo) {
        this.repo = repo;
    }

    public ResponseEntity<UserEntity> registerUser(@RequestBody UserEntity user) {
        user.setCreatedAt(System.currentTimeMillis());
        UserEntity saved = repo.save(user);
        log.info("Neuer User registriert: {}", saved.getEmail());
        return ResponseEntity.ok(saved);
    }

    public List<UserEntity> getAllUsers() {
        log.info("Alle Benutzer abgefragt – Thread: {}", Thread.currentThread().getName());
        return repo.findAll();
    }

    public ResponseEntity<UserEntity> getUserById(@PathVariable Long id) {
        return repo.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    public ResponseEntity<UserEntity> getByEmail(@RequestParam String email) {
        return repo.findByEmail(email)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }
}
