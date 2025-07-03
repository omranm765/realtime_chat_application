package de.htwsaar.messagebroker.controller;

import de.htwsaar.messagebroker.entity.UserEntity;
import de.htwsaar.messagebroker.repository.UserRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/users")
public class UserController {

    private final UserRepository repo;

    public UserController(UserRepository repo) {
        this.repo = repo;
    }

    @PostMapping
    public ResponseEntity<UserEntity> registerUser(@RequestBody UserEntity user) {
        user.setCreatedAt(System.currentTimeMillis());
        UserEntity saved = repo.save(user);
        return ResponseEntity.ok(saved);
    }

    @GetMapping
    public List<UserEntity> getAllUsers() {
        System.out.println("UserController → getAllUsers() laeuft in Thread: " + Thread.currentThread().getName());
        return repo.findAll();
    }

    @GetMapping("/{id}")
    public ResponseEntity<UserEntity> getUserById(@PathVariable Long id) {
        return repo.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/by-email")
    public ResponseEntity<UserEntity> getByEmail(@RequestParam String email) {
        return repo.findByEmail(email)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }
}
