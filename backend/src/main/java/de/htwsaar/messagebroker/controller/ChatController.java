package de.htwsaar.messagebroker.controller;

import de.htwsaar.messagebroker.entity.ChatMessageEntity;
import de.htwsaar.messagebroker.repository.ChatMessageRepository;
import de.htwsaar.messagebroker.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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

    private static final Logger log = LoggerFactory.getLogger(ChatController.class);

    @MessageMapping("/chat/send")
    public void handleMessage(ChatMessageEntity message) {
        log.info("Empfange Nachricht von User-ID {}, Event '{}', Thread: {}",
                message.getSenderId(), message.getEventType(), Thread.currentThread().getName());
        userRepository.findById(message.getSenderId()).ifPresent(user -> {
            message.setSenderDisplayName(user.getDisplayName());
        });

        repository.save(message);
        messagingTemplate.convertAndSend("/topic/" + message.getEventType(), message);

        log.info("Nachricht an Topic '{}' gesendet", message.getEventType());
    }

}
