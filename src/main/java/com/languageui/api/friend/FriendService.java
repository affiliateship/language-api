package com.languageui.api.friend;

import java.util.List;
import java.util.UUID;

import com.languageui.api.user.UserAccount;
import com.languageui.api.user.UserService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class FriendService {
    private final FriendRepository repository;
    private final UserService userService;

    public FriendService(FriendRepository repository, UserService userService) {
        this.repository = repository;
        this.userService = userService;
    }

    public FriendRequestResponse request(UUID requesterId, UUID recipientId) {
        if (requesterId.equals(recipientId)) {
            throw new IllegalArgumentException("You cannot add yourself as a friend");
        }
        UserAccount recipient = userService.findById(recipientId);
        if (repository.areFriends(requesterId, recipient.id())) {
            throw new IllegalStateException("This user is already your friend");
        }
        if (repository.requestExists(requesterId, recipient.id())) {
            throw new IllegalStateException("A pending friend request already exists");
        }
        FriendRepository.PendingRequest request = repository.createRequest(requesterId, recipient.id());
        return response(request);
    }

    public List<FriendRequestResponse> incoming(UUID userId) {
        return repository.incoming(userId).stream().map(this::response).toList();
    }

    @Transactional
    public List<FriendSummary> accept(UUID userId, UUID requestId) {
        FriendRepository.PendingRequest request = pending(requestId);
        if (!request.recipientId().equals(userId)) {
            throw new IllegalArgumentException("This friend request does not belong to you");
        }
        repository.accept(request);
        return friends(userId);
    }

    public void declineOrCancel(UUID userId, UUID requestId) {
        FriendRepository.PendingRequest request = pending(requestId);
        if (!request.recipientId().equals(userId) && !request.requesterId().equals(userId)) {
            throw new IllegalArgumentException("This friend request does not belong to you");
        }
        repository.deleteRequest(requestId);
    }

    public List<FriendSummary> friends(UUID userId) {
        return repository.friendIds(userId).stream().map(this::summary).toList();
    }

    public void remove(UUID userId, UUID friendId) {
        if (!repository.areFriends(userId, friendId)) {
            throw new IllegalArgumentException("Friend not found: " + friendId);
        }
        repository.removeFriend(userId, friendId);
    }

    private FriendRepository.PendingRequest pending(UUID id) {
        FriendRepository.PendingRequest request = repository.findRequest(id)
                .orElseThrow(() -> new IllegalArgumentException("Friend request not found: " + id));
        if (!request.status().equals("PENDING")) {
            throw new IllegalStateException("Friend request is no longer pending");
        }
        return request;
    }

    private FriendRequestResponse response(FriendRepository.PendingRequest request) {
        return new FriendRequestResponse(request.id(), summary(request.requesterId()),
                request.createdAt());
    }

    private FriendSummary summary(UUID userId) {
        UserAccount user = userService.findById(userId);
        return new FriendSummary(user.id(), user.firstName(), user.lastName(), user.displayName(),
                userService.learningLanguages(user.id()));
    }
}
