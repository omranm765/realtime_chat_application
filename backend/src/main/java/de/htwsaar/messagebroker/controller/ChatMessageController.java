package de.htwsaar.messagebroker.controller;

import de.htwsaar.messagebroker.entity.ChatMessageEntity;
import de.htwsaar.messagebroker.repository.ChatMessageRepository;
import org.springframework.web.bind.annotation.*;
import de.htwsaar.messagebroker.repository.UserRepository;
import de.htwsaar.messagebroker.entity.UserEntity;


import java.util.List;

@RestController
@RequestMapping("/api")
public class ChatMessageController {
    private Long senderId;

    private final ChatMessageRepository chatMessageRepository;
    private final UserRepository userRepo;

    public ChatMessageController(ChatMessageRepository chatMessageRepository, UserRepository userRepo) {
        this.chatMessageRepository = chatMessageRepository;
        this.userRepo = userRepo;
    }

    @GetMapping("/messages/{event}")
    public List<ChatMessageEntity> getMessages(@PathVariable String event) {
        System.out.println("ChatMessageController → getMessages() laeuft in Thread: " + Thread.currentThread().getName());
        List<ChatMessageEntity> messages = chatMessageRepository.findByEventTypeOrderByTimestampAsc(event);

        for (ChatMessageEntity msg : messages) {
            userRepo.findById(msg.getSenderId()).ifPresent(user ->
                    msg.setSenderDisplayName(user.getDisplayName())
            );
        }

        return messages;

    }
}
