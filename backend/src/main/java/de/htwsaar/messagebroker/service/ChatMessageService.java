package de.htwsaar.messagebroker.service;

import de.htwsaar.messagebroker.controller.ClientSubscriptionController;
import de.htwsaar.messagebroker.entity.ChatMessageEntity;
import de.htwsaar.messagebroker.entity.MessageDeliveryEntity;
import de.htwsaar.messagebroker.repository.ChatMessageRepository;
import de.htwsaar.messagebroker.repository.ClientRepository;
import de.htwsaar.messagebroker.repository.MessageDeliveryRepository;
import de.htwsaar.messagebroker.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Service
public class ChatMessageService {

    private Long senderId;

    private final ChatMessageRepository chatMessageRepository;
    private final UserRepository userRepo;
    private final ClientRepository clientRepo;
    private final MessageDeliveryRepository deliveryRepo;
    private static final Logger log = LoggerFactory.getLogger(ClientSubscriptionController.class);

    public ChatMessageService(ChatMessageRepository chatMessageRepository,
                                 UserRepository userRepo,
                                 ClientRepository clientRepo,
                                 MessageDeliveryRepository deliveryRepo) {
        this.chatMessageRepository = chatMessageRepository;
        this.userRepo = userRepo;
        this.clientRepo = clientRepo;
        this.deliveryRepo = deliveryRepo;
    }

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
