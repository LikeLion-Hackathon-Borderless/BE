package com.likelion.asyncalign.messenger.application;

import com.likelion.asyncalign.messenger.domain.DeliveryStatus;
import com.likelion.asyncalign.messenger.domain.MessageRepository;
import java.time.Instant;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class ScheduledMessageDispatcher {

    private final MessageRepository messageRepository;

    public ScheduledMessageDispatcher(MessageRepository messageRepository) {
        this.messageRepository = messageRepository;
    }

    @Scheduled(fixedDelayString = "${app.messaging.scheduled-dispatch-delay-ms:5000}")
    @Transactional
    public void dispatchDueMessages() {
        messageRepository.findAllByDeliveryStatusAndScheduledForLessThanEqual(
                        DeliveryStatus.SCHEDULED,
                        Instant.now())
                .forEach(message -> {
                    message.markDueAsSent();
                    message.getConversation().touch(Instant.now());
                });
    }
}
