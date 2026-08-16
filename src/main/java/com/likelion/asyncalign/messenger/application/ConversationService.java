package com.likelion.asyncalign.messenger.application;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import com.likelion.asyncalign.global.error.ApiException;
import com.likelion.asyncalign.global.error.ErrorCode;
import com.likelion.asyncalign.messenger.domain.Conversation;
import com.likelion.asyncalign.messenger.domain.ConversationMember;
import com.likelion.asyncalign.messenger.domain.ConversationMemberRepository;
import com.likelion.asyncalign.messenger.domain.ConversationRepository;
import com.likelion.asyncalign.messenger.domain.Message;
import com.likelion.asyncalign.messenger.domain.MessageRepository;
import com.likelion.asyncalign.messenger.dto.ConversationResponse;
import com.likelion.asyncalign.user.application.UserService;
import com.likelion.asyncalign.user.domain.User;
import com.likelion.asyncalign.user.dto.UserSummaryResponse;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class ConversationService {

    private final ConversationRepository conversationRepository;
    private final ConversationMemberRepository memberRepository;
    private final MessageRepository messageRepository;
    private final UserService userService;

    public ConversationService(
            ConversationRepository conversationRepository,
            ConversationMemberRepository memberRepository,
            MessageRepository messageRepository,
            UserService userService
    ) {
        this.conversationRepository = conversationRepository;
        this.memberRepository = memberRepository;
        this.messageRepository = messageRepository;
        this.userService = userService;
    }

    @Transactional
    public ConversationResponse createDirect(UUID currentUserId, UUID otherUserId) {
        if (currentUserId.equals(otherUserId)) {
            throw new ApiException(ErrorCode.DIRECT_CONVERSATION_WITH_SELF, "자기 자신과 DM을 만들 수 없습니다.");
        }

        User currentUser = userService.getUser(currentUserId);
        User otherUser = userService.getUser(otherUserId);
        String directKey = Conversation.directKey(currentUserId, otherUserId);
        Conversation conversation = conversationRepository.findByDirectKey(directKey)
                .orElseGet(() -> createConversation(currentUser, otherUser));
        return toResponse(conversation, currentUserId);
    }

    public List<ConversationResponse> getConversations(UUID currentUserId) {
        return memberRepository.findAllForUser(currentUserId).stream()
                .map(member -> toResponse(member.getConversation(), currentUserId))
                .toList();
    }

    @Transactional
    public void markRead(UUID conversationId, UUID currentUserId) {
        ConversationMember member = getMembership(conversationId, currentUserId);
        member.markRead(Instant.now());
    }

    public ConversationMember getMembership(UUID conversationId, UUID userId) {
        return memberRepository.findByConversationIdAndUserId(conversationId, userId)
                .orElseThrow(() -> new ApiException(
                        ErrorCode.CONVERSATION_NOT_FOUND,
                        "대화를 찾을 수 없거나 접근 권한이 없습니다."));
    }

    private Conversation createConversation(User currentUser, User otherUser) {
        Conversation conversation = conversationRepository.save(Conversation.direct(currentUser.getId(), otherUser.getId()));
        memberRepository.saveAll(List.of(
                new ConversationMember(conversation, currentUser),
                new ConversationMember(conversation, otherUser)));
        return conversation;
    }

    private ConversationResponse toResponse(Conversation conversation, UUID currentUserId) {
        List<ConversationMember> members = memberRepository.findAllWithUserByConversationId(conversation.getId());
        ConversationMember currentMember = members.stream()
                .filter(member -> member.getUser().getId().equals(currentUserId))
                .findFirst()
                .orElseThrow(() -> new ApiException(ErrorCode.ACCESS_DENIED, "대화에 접근할 수 없습니다."));
        User otherUser = members.stream()
                .map(ConversationMember::getUser)
                .filter(user -> !user.getId().equals(currentUserId))
                .findFirst()
                .orElseThrow(() -> new ApiException(ErrorCode.USER_NOT_FOUND, "대화 상대를 찾을 수 없습니다."));

        Message latest = messageRepository.findFirstByConversationIdOrderByCreatedAtDesc(conversation.getId())
                .orElse(null);
        ConversationResponse.LatestMessage latestResponse = latest == null ? null : new ConversationResponse.LatestMessage(
                latest.getId(),
                latest.getSender().getId(),
                latest.getContent(),
                latest.getCreatedAt());
        Instant lastReadAt = currentMember.getLastReadAt() == null ? Instant.EPOCH : currentMember.getLastReadAt();
        long unreadCount = messageRepository.countByConversationIdAndCreatedAtAfterAndSenderIdNot(
                conversation.getId(), lastReadAt, currentUserId);

        return new ConversationResponse(
                conversation.getId(),
                conversation.getType().name(),
                UserSummaryResponse.from(otherUser),
                latestResponse,
                unreadCount,
                conversation.getLastMessageAt());
    }
}
