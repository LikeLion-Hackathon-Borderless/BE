package com.likelion.asyncalign.user.application;

import java.util.List;
import java.util.UUID;

import com.likelion.asyncalign.global.error.ApiException;
import com.likelion.asyncalign.global.error.ErrorCode;
import com.likelion.asyncalign.user.domain.User;
import com.likelion.asyncalign.user.domain.UserRepository;
import com.likelion.asyncalign.user.dto.UserResponse;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class UserService {

    private final UserRepository userRepository;

    public UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public User getUser(UUID userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new ApiException(ErrorCode.USER_NOT_FOUND, "사용자를 찾을 수 없습니다."));
    }

    public UserResponse getMe(UUID userId) {
        return UserResponse.from(getUser(userId));
    }

    public List<UserResponse> search(UUID currentUserId, String query, int size) {
        int safeSize = Math.clamp(size, 1, 50);
        String keyword = query == null ? "" : query.trim();
        return userRepository
                .findByDisplayNameContainingIgnoreCaseOrEmailContainingIgnoreCase(
                        keyword,
                        keyword,
                        PageRequest.of(0, safeSize))
                .stream()
                .filter(user -> !user.getId().equals(currentUserId))
                .map(UserResponse::from)
                .toList();
    }
}
