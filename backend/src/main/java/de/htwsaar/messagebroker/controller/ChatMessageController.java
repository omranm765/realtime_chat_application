package de.htwsaar.messagebroker.controller;

import de.htwsaar.messagebroker.entity.ChatMessageEntity;
import de.htwsaar.messagebroker.entity.MessageDeliveryEntity;
import de.htwsaar.messagebroker.repository.ChatMessageRepository;
import de.htwsaar.messagebroker.repository.ClientRepository;
import de.htwsaar.messagebroker.repository.MessageDeliveryRepository;
import de.htwsaar.messagebroker.service.ChatMessageService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.*;
import de.htwsaar.messagebroker.repository.UserRepository;
import de.htwsaar.messagebroker.entity.UserEntity;


import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@RestController
@RequestMapping("/api")
public class ChatMessageController {
    private Long senderId;

    private final ChatMessageRepository chatMessageRepository;
    private final UserRepository userRepo;
    private final ClientRepository clientRepo;
    private final MessageDeliveryRepository deliveryRepo;
    private final ChatMessageService chatMessageService;
    private static final Logger log = LoggerFactory.getLogger(ClientSubscriptionController.class);

    public ChatMessageController(ChatMessageRepository chatMessageRepository,
                                 UserRepository userRepo,
                                 ClientRepository clientRepo,
                                 MessageDeliveryRepository deliveryRepo,
                                 ChatMessageService chatMessageService) {
        this.chatMessageRepository = chatMessageRepository;
        this.userRepo = userRepo;
        this.clientRepo = clientRepo;
        this.deliveryRepo = deliveryRepo;
        this.chatMessageService = chatMessageService;
    }

    @GetMapping("/messages/{event}")
    public List<ChatMessageEntity> getMessages(@PathVariable String event) {
        return chatMessageService.getMessages(event);
    }

    @GetMapping("/messages/undelivered")
    public List<ChatMessageEntity> getUndeliveredMessages(@RequestParam String clientId) {
        return chatMessageService.getUndeliveredMessages(clientId);
    }

    @PostMapping("/messages/mark-delivered")
    public void markMessagesAsDelivered(@RequestParam String clientId, @RequestBody List<Long> messageIds) {
        chatMessageService.markMessagesAsDelivered(clientId, messageIds);
    }
}