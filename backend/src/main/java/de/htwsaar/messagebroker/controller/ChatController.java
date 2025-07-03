package de.htwsaar.messagebroker.controller;

import de.htwsaar.messagebroker.entity.ChatMessageEntity;
import de.htwsaar.messagebroker.repository.ChatMessageRepository;
import de.htwsaar.messagebroker.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;

@Controller
public class ChatController {

    @Autowired
    private SimpMessagingTemplate messagingTemplate;

    @Autowired
    private ChatMessageRepository repository;

    @Autowired
    private UserRepository userRepository;

    @MessageMapping("/chat/send")
    public void handleMessage(ChatMessageEntity message) {
        System.out.println("ChatController → handleMessage() laeuft in Thread: " + Thread.currentThread().getName());
        userRepository.findById(message.getSenderId()).ifPresent(user -> {
            message.setSenderDisplayName(user.getDisplayName());
        });

        repository.save(message);
        messagingTemplate.convertAndSend("/topic/" + message.getEventType(), message);
    }

}
