package com.platform.user.api;

import com.platform.shared.security.TenantContext;
import com.platform.user.internal.User;
import com.platform.user.internal.UserService;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/users")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping("/me")
    public ResponseEntity<UserResponse> getCurrentUser() {
        Optional<User> user = userService.getCurrentUser();
        return user.map(u -> ResponseEntity.ok(UserResponse.fromUser(u)))
                   .orElse(ResponseEntity.status(HttpStatus.UNAUTHORIZED).build());
    }

    @GetMapping("/{userId}")
    public ResponseEntity<UserResponse> getUserById(@PathVariable UUID userId) {
        Optional<User> user = userService.findById(userId);
        return user.map(u -> ResponseEntity.ok(UserResponse.fromUser(u)))
                   .orElse(ResponseEntity.notFound().build());
    }

    @PutMapping("/me/profile")
    public ResponseEntity<UserResponse> updateProfile(@Valid @RequestBody UpdateProfileRequest request) {
        UUID currentUserId = TenantContext.getCurrentUserId();
        if (currentUserId == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        User updatedUser = userService.updateProfile(
            currentUserId,
            request.name(),
            request.preferences()
        );

        return ResponseEntity.ok(UserResponse.fromUser(updatedUser));
    }

    @PutMapping("/me/preferences")
    public ResponseEntity<UserResponse> updatePreferences(@Valid @RequestBody UpdatePreferencesRequest request) {
        UUID currentUserId = TenantContext.getCurrentUserId();
        if (currentUserId == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        User updatedUser = userService.updatePreferences(currentUserId, request.preferences());
        return ResponseEntity.ok(UserResponse.fromUser(updatedUser));
    }

    @DeleteMapping("/me")
    public ResponseEntity<Void> deleteCurrentUser() {
        UUID currentUserId = TenantContext.getCurrentUserId();
        if (currentUserId == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        userService.deleteUser(currentUserId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/search")
    public ResponseEntity<List<UserResponse>> searchUsers(@RequestParam String name) {
        List<User> users = userService.searchUsersByName(name);
        List<UserResponse> responses = users.stream()
            .map(UserResponse::fromUser)
            .toList();
        return ResponseEntity.ok(responses);
    }

    @GetMapping
    public ResponseEntity<PagedUserResponse> getAllUsers(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDirection) {

        Page<User> userPage = userService.findAllUsers(page, size, sortBy, sortDirection);

        List<UserResponse> users = userPage.getContent().stream()
            .map(UserResponse::fromUser)
            .toList();

        PagedUserResponse response = new PagedUserResponse(
            users,
            userPage.getNumber(),
            userPage.getSize(),
            userPage.getTotalElements(),
            userPage.getTotalPages(),
            userPage.isFirst(),
            userPage.isLast()
        );

        return ResponseEntity.ok(response);
    }

    @GetMapping("/recent")
    public ResponseEntity<List<UserResponse>> getRecentUsers(@RequestParam String since) {
        try {
            Instant sinceInstant = Instant.parse(since);
            List<User> users = userService.getRecentlyActiveUsers(sinceInstant);
            List<UserResponse> responses = users.stream()
                .map(UserResponse::fromUser)
                .toList();
            return ResponseEntity.ok(responses);
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @GetMapping("/statistics")
    public ResponseEntity<UserService.UserStatistics> getUserStatistics() {
        UserService.UserStatistics stats = userService.getUserStatistics();
        return ResponseEntity.ok(stats);
    }

    @GetMapping("/count")
    public ResponseEntity<Long> countActiveUsers() {
        long count = userService.countActiveUsers();
        return ResponseEntity.ok(count);
    }

    @GetMapping("/providers/{provider}")
    public ResponseEntity<List<UserResponse>> getUsersByProvider(@PathVariable String provider) {
        List<User> users = userService.findUsersByProvider(provider);
        List<UserResponse> responses = users.stream()
            .map(UserResponse::fromUser)
            .toList();
        return ResponseEntity.ok(responses);
    }

    @PostMapping("/{userId}/restore")
    public ResponseEntity<UserResponse> restoreUser(@PathVariable UUID userId) {
        User restoredUser = userService.restoreUser(userId);
        return ResponseEntity.ok(UserResponse.fromUser(restoredUser));
    }

    public record UserResponse(
        UUID id,
        String email,
        String name,
        String provider,
        Map<String, Object> preferences,
        Instant createdAt,
        Instant lastActiveAt
    ) {
        public static UserResponse fromUser(User user) {
            return new UserResponse(
                user.getId(),
                user.getEmail().getValue(),
                user.getName(),
                user.getProvider(),
                user.getPreferences(),
                user.getCreatedAt(),
                user.getLastActiveAt()
            );
        }
    }

    public record UpdateProfileRequest(
        @NotBlank String name,
        Map<String, Object> preferences
    ) {}

    public record UpdatePreferencesRequest(
        @NotNull Map<String, Object> preferences
    ) {}

    public record PagedUserResponse(
        List<UserResponse> users,
        int page,
        int size,
        long totalElements,
        int totalPages,
        boolean first,
        boolean last
    ) {}
}