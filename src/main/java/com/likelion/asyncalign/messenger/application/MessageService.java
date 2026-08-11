package com.likelion.asyncalign.messenger.application;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import com.likelion.asyncalign.messenger.domain.Conversation;
import com.likelion.asyncalign.messenger.domain.ConversationMember;
import com.likelion.asyncalign.messenger.domain.Message;
import com.likelion.asyncalign.messenger.domain.MessageRepository;
import com.likelion.asyncalign.messenger.dto.MessagePageResponse;
import com.likelion.asyncalign.messenger.dto.MessageResponse;
import com.likelion.asyncalign.user.application.UserService;
import com.likelion.asyncalign.user.domain.User;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class MessageService {

    private final MessageRepository messageRepository;
    private final ConversationService conversationService;
    private final UserService userService;

    public MessageService(
            MessageRepository messageRepository,
            ConversationService conversationService,
            UserService userService
    ) {
        this.messageRepository = messageRepository;
        this.conversationService = conversationService;
        this.userService = userService;
    }

    public MessagePageResponse getMessages(UUID conversationId, UUID currentUserId, Instant before, int size) {
        conversationService.getMembership(conversationId, currentUserId);
        User viewer = userService.getUser(currentUserId);
        int safeSize = Math.clamp(size, 1, 100);
        Instant cursor = before == null ? Instant.now().plusSeconds(1) : before;
        List<Message> result = messageRepository.findPageBefore(
                conversationId,
                cursor,
                PageRequest.of(0, safeSize + 1));

        boolean hasMore = result.size() > safeSize;
        List<Message> page = new ArrayList<>(result.subList(0, Math.min(result.size(), safeSize)));
        Instant nextBefore = hasMore && !page.isEmpty() ? page.getLast().getCreatedAt() : null;
        Collections.reverse(page);
        List<MessageResponse> responses = page.stream()
                .map(message -> MessageResponse.from(message, viewer))
                .toList();
        return new MessagePageResponse(responses, hasMore, nextBefore);
    }

    @Transactional
    public MessageResponse send(UUID conversationId, UUID currentUserId, String rawContent) {
        ConversationMember membership = conversationService.getMembership(conversationId, currentUserId);
        User sender = membership.getUser();
        Conversation conversation = membership.getConversation();
        Message message = messageRepository.saveAndFlush(new Message(conversation, sender, rawContent.trim()));
        conversation.touch(message.getCreatedAt());
        membership.markRead(message.getCreatedAt());
        return MessageResponse.from(message, sender);
    }
}
