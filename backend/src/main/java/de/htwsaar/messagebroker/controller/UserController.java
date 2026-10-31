package de.htwsaar.messagebroker.controller;

import de.htwsaar.messagebroker.entity.UserEntity;
import de.htwsaar.messagebroker.repository.UserRepository;
import de.htwsaar.messagebroker.service.UserService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/users")
public class UserController {

    private final UserRepository repo;
    private static final Logger log = LoggerFactory.getLogger(UserController.class);
    private final UserService userService;

    public UserController(UserRepository repo,
                          UserService userService) {
        this.repo = repo;
        this.userService = userService;
    }

    @PostMapping
    public ResponseEntity<UserEntity> registerUser(@RequestBody UserEntity user) {
        return userService.registerUser(user);
    }

    @GetMapping
    public List<UserEntity> getAllUsers() {
        return userService.getAllUsers();
    }

    @GetMapping("/{id}")
    public ResponseEntity<UserEntity> getUserById(@PathVariable Long id) {
        return userService.getUserById(id);
    }

    @GetMapping("/by-email")
    public ResponseEntity<UserEntity> getByEmail(@RequestParam String email) {
        return userService.getByEmail(email);
    }
    // ToDO getById Not ByEmail
}
