package de.htwsaar.messagebroker.controller;

import de.htwsaar.messagebroker.entity.ChatMessageEntity;
import de.htwsaar.messagebroker.entity.MessageDeliveryEntity;
import de.htwsaar.messagebroker.repository.ChatMessageRepository;
import de.htwsaar.messagebroker.repository.ClientRepository;
import de.htwsaar.messagebroker.repository.MessageDeliveryRepository;
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
    private static final Logger log = LoggerFactory.getLogger(ClientSubscriptionController.class);

    public ChatMessageController(ChatMessageRepository chatMessageRepository,
                                 UserRepository userRepo,
                                 ClientRepository clientRepo,
                                 MessageDeliveryRepository deliveryRepo) {
        this.chatMessageRepository = chatMessageRepository;
        this.userRepo = userRepo;
        this.clientRepo = clientRepo;
        this.deliveryRepo = deliveryRepo;
    }

    @GetMapping("/messages/{event}")
    public List<ChatMessageEntity> getMessages(@PathVariable String event) {
        log.info("Nachrichtenabfrage für Event '{}', Thread: {}", event, Thread.currentThread().getName());
        List<ChatMessageEntity> messages = chatMessageRepository.findByEventTypeOrderByTimestampAsc(event);

        for (ChatMessageEntity msg : messages) {
            userRepo.findById(msg.getSenderId()).ifPresent(user ->
                    msg.setSenderDisplayName(user.getDisplayName())
            );
        }

        return messages;
    }

    @GetMapping("/messages/undelivered")
    public List<ChatMessageEntity> getUndeliveredMessages(@RequestParam String clientId) {
        var subscribedTopics = clientRepo.findById(clientId)
                .map(c -> c.getSubscribedEventTypes())
                .orElse(Collections.emptySet());

        if (subscribedTopics.isEmpty()) return List.of();

        var allMessages = chatMessageRepository
                .findByEventTypeInOrderByTimestampAsc(new ArrayList<>(subscribedTopics));

        for (ChatMessageEntity msg : allMessages) {
            userRepo.findById(msg.getSenderId()).ifPresent(user ->
                    msg.setSenderDisplayName(user.getDisplayName())
            );
        }

        return allMessages.stream()
                .filter(msg -> !deliveryRepo.existsByClientIdAndMessageId(clientId, msg.getId()))
                .toList();
    }

    @PostMapping("/messages/mark-delivered")
    public void markMessagesAsDelivered(@RequestParam String clientId, @RequestBody List<Long> messageIds) {
        long now = System.currentTimeMillis();
        for (Long messageId : messageIds) {
            if (!deliveryRepo.existsByClientIdAndMessageId(clientId, messageId)) {
                MessageDeliveryEntity delivery = new MessageDeliveryEntity();
                delivery.setClientId(clientId);
                delivery.setMessageId(messageId);
                delivery.setDeliveredAt(now);
                deliveryRepo.save(delivery);
            }
        }
        log.info("Client '{}' hat {} Nachrichten als zugestellt markiert.", clientId, messageIds.size());
    }
}