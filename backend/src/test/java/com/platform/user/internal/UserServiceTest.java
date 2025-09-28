package com.platform.user.internal;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.lang.reflect.Field;
import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.platform.audit.internal.AuditService;
import com.platform.shared.security.TenantContext;
import com.platform.shared.types.Email;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private AuditService auditService;

    @InjectMocks
    private UserService userService;

    private UUID userId;
    private String testEmail;
    private String testName;
    private String testProvider;
    private String testProviderId;
    private Map<String, Object> testPreferences;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        testEmail = "test@example.com";
        testName = "Test User";
        testProvider = "google";
        testProviderId = "google123";
        testPreferences = Map.of("theme", "dark", "language", "en");

        // Set up tenant context
        TenantContext.setTenantInfo(UUID.randomUUID(), "test-org", userId);
    }

    @Test
    void createUser_withBasicInfo_succeeds() {
        // Given
        when(userRepository.existsByEmailAndDeletedAtIsNull(testEmail)).thenReturn(false);
        when(userRepository.findByProviderAndProviderId(testProvider, testProviderId)).thenReturn(Optional.empty());

        User savedUser = createMockUser();
        when(userRepository.save(any(User.class))).thenReturn(savedUser);

        // When
        User result = userService.createUser(testEmail, testName, testProvider, testProviderId);

        // Then
        assertNotNull(result);
        assertEquals(testEmail, result.getEmail().getValue());
        assertEquals(testName, result.getName());
        assertEquals(testProvider, result.getProvider());
        assertEquals(testProviderId, result.getProviderId());

        verify(userRepository).existsByEmailAndDeletedAtIsNull(testEmail);
        verify(userRepository).findByProviderAndProviderId(testProvider, testProviderId);
        verify(userRepository).save(any(User.class));
        verify(auditService, times(2)).logEvent(anyString(), anyString(), anyString(), anyString(),
                any(Map.class), any(), anyString(), anyString(), any());
    }

    @Test
    void createUser_withPreferences_succeeds() {
        // Given
        when(userRepository.existsByEmailAndDeletedAtIsNull(testEmail)).thenReturn(false);
        when(userRepository.findByProviderAndProviderId(testProvider, testProviderId)).thenReturn(Optional.empty());

        User savedUser = createMockUser();
        when(userRepository.save(any(User.class))).thenReturn(savedUser);

        // When
        User result = userService.createUser(testEmail, testName, testProvider, testProviderId, testPreferences);

        // Then
        assertNotNull(result);
        assertEquals(testEmail, result.getEmail().getValue());
        assertEquals(testName, result.getName());
        assertEquals(testProvider, result.getProvider());
        assertEquals(testProviderId, result.getProviderId());

        verify(userRepository).existsByEmailAndDeletedAtIsNull(testEmail);
        verify(userRepository).findByProviderAndProviderId(testProvider, testProviderId);
        verify(userRepository).save(any(User.class));
        verify(auditService, times(3)).logEvent(anyString(), anyString(), anyString(), anyString(),
                any(Map.class), any(), anyString(), anyString(), any());
    }

    @Test
    void createUser_withDuplicateEmail_throwsException() {
        // Given
        when(userRepository.existsByEmailAndDeletedAtIsNull(testEmail)).thenReturn(true);

        // When & Then
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> userService.createUser(testEmail, testName, testProvider, testProviderId));

        assertEquals("User with email already exists: " + testEmail, exception.getMessage());

        verify(userRepository).existsByEmailAndDeletedAtIsNull(testEmail);
        verify(userRepository, never()).save(any(User.class));
        verify(auditService, times(2)).logEvent(anyString(), anyString(), anyString(), anyString(),
                any(Map.class), any(), anyString(), anyString(), any());
    }

    @Test
    void createUser_withDuplicateProviderId_throwsException() {
        // Given
        when(userRepository.existsByEmailAndDeletedAtIsNull(testEmail)).thenReturn(false);

        User existingUser = createMockUser();
        when(userRepository.findByProviderAndProviderId(testProvider, testProviderId))
                .thenReturn(Optional.of(existingUser));

        // When & Then
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> userService.createUser(testEmail, testName, testProvider, testProviderId));

        assertEquals("User with provider already exists: " + testProvider + ":" + testProviderId,
                exception.getMessage());

        verify(userRepository).existsByEmailAndDeletedAtIsNull(testEmail);
        verify(userRepository).findByProviderAndProviderId(testProvider, testProviderId);
        verify(userRepository, never()).save(any(User.class));
        verify(auditService, times(2)).logEvent(anyString(), anyString(), anyString(), anyString(),
                any(Map.class), any(), anyString(), anyString(), any());
    }

    @Test
    void updateProfile_withValidData_succeeds() {
        // Given
        User existingUser = createMockUser();
        when(userRepository.findById(userId)).thenReturn(Optional.of(existingUser));
        when(userRepository.save(any(User.class))).thenReturn(existingUser);

        String newName = "Updated Name";
        Map<String, Object> newPreferences = Map.of("theme", "light", "notifications", true);

        // When
        User result = userService.updateProfile(userId, newName, newPreferences);

        // Then
        assertNotNull(result);
        verify(userRepository).findById(userId);
        verify(userRepository).save(any(User.class));
        verify(auditService, times(2)).logEvent(anyString(), anyString(), anyString(), anyString(),
                any(Map.class), any(), anyString(), anyString(), any());
    }

    @Test
    void updateProfile_userNotFound_throwsException() {
        // Given
        when(userRepository.findById(userId)).thenReturn(Optional.empty());

        // When & Then
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> userService.updateProfile(userId, testName, testPreferences));

        assertEquals("User not found: " + userId, exception.getMessage());

        verify(userRepository).findById(userId);
        verify(userRepository, never()).save(any(User.class));
        verifyNoInteractions(auditService);
    }

    @Test
    void updateProfile_unauthorizedAccess_throwsSecurityException() {
        // Given
        User existingUser = createMockUser();
        setField(existingUser, "id", UUID.randomUUID()); // Different ID than tenant context
        when(userRepository.findById(userId)).thenReturn(Optional.of(existingUser));

        // When & Then
        SecurityException exception = assertThrows(SecurityException.class,
                () -> userService.updateProfile(userId, testName, testPreferences));

        assertEquals("Access denied - cannot modify other user's profile", exception.getMessage());

        verify(userRepository).findById(userId);
        verify(userRepository, never()).save(any(User.class));
        verifyNoInteractions(auditService);
    }

    @Test
    void deleteUser_withValidUser_succeeds() {
        // Given
        User existingUser = createMockUser();
        when(userRepository.findById(userId)).thenReturn(Optional.of(existingUser));
        when(userRepository.save(any(User.class))).thenReturn(existingUser);

        // When
        userService.deleteUser(userId);

        // Then
        verify(userRepository).findById(userId);
        verify(userRepository).save(any(User.class));
        verify(auditService, times(2)).logEvent(anyString(), anyString(), anyString(), anyString(),
                any(Map.class), any(), anyString(), anyString(), any());
    }

    @Test
    void deleteUser_userNotFound_throwsException() {
        // Given
        when(userRepository.findById(userId)).thenReturn(Optional.empty());

        // When & Then
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> userService.deleteUser(userId));

        assertEquals("User not found: " + userId, exception.getMessage());

        verify(userRepository).findById(userId);
        verify(userRepository, never()).save(any(User.class));
        verifyNoInteractions(auditService);
    }

    @Test
    void deleteUser_unauthorizedAccess_throwsSecurityException() {
        // Given
        User existingUser = createMockUser();
        setField(existingUser, "id", UUID.randomUUID()); // Different ID than tenant context
        when(userRepository.findById(userId)).thenReturn(Optional.of(existingUser));

        // When & Then
        SecurityException exception = assertThrows(SecurityException.class,
                () -> userService.deleteUser(userId));

        assertEquals("Access denied - cannot delete other users", exception.getMessage());

        verify(userRepository).findById(userId);
        verify(userRepository, never()).save(any(User.class));
        verifyNoInteractions(auditService);
    }

    @Test
    void restoreUser_withValidDeletedUser_succeeds() {
        // Given
        User deletedUser = createMockUser();
        setField(deletedUser, "deletedAt", Instant.now());

        when(userRepository.findById(userId)).thenReturn(Optional.of(deletedUser));
        when(userRepository.existsByEmailAndDeletedAtIsNull(testEmail)).thenReturn(false);
        doNothing().when(userRepository).restoreUser(userId);

        User restoredUser = createMockUser();
        setField(restoredUser, "deletedAt", null);
        when(userRepository.findById(userId)).thenReturn(Optional.of(restoredUser));

        // When
        User result = userService.restoreUser(userId);

        // Then
        assertNotNull(result);
        verify(userRepository, times(2)).findById(userId);
        verify(userRepository).existsByEmailAndDeletedAtIsNull(testEmail);
        verify(userRepository).restoreUser(userId);
        verify(auditService, times(2)).logEvent(anyString(), anyString(), anyString(), anyString(),
                any(Map.class), any(), anyString(), anyString(), any());
    }

    @Test
    void restoreUser_userNotFound_throwsException() {
        // Given
        when(userRepository.findById(userId)).thenReturn(Optional.empty());

        // When & Then
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> userService.restoreUser(userId));

        assertEquals("User not found: " + userId, exception.getMessage());

        verify(userRepository).findById(userId);
        verify(userRepository, never()).restoreUser(any());
        verify(auditService).logEvent(anyString(), anyString(), anyString(), anyString(),
                any(Map.class), any(), anyString(), anyString(), any());
    }

    @Test
    void restoreUser_userNotDeleted_throwsException() {
        // Given
        User activeUser = createMockUser();
        when(userRepository.findById(userId)).thenReturn(Optional.of(activeUser));

        // When & Then
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> userService.restoreUser(userId));

        assertEquals("User is not deleted: " + userId, exception.getMessage());

        verify(userRepository).findById(userId);
        verify(userRepository, never()).restoreUser(any());
        verify(auditService).logEvent(anyString(), anyString(), anyString(), anyString(),
                any(Map.class), any(), anyString(), anyString(), any());
    }

    @Test
    void restoreUser_emailConflict_throwsException() {
        // Given
        User deletedUser = createMockUser();
        setField(deletedUser, "deletedAt", Instant.now());

        when(userRepository.findById(userId)).thenReturn(Optional.of(deletedUser));
        when(userRepository.existsByEmailAndDeletedAtIsNull(testEmail)).thenReturn(true);

        // When & Then
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> userService.restoreUser(userId));

        assertEquals("Cannot restore user - email already in use: " + testEmail, exception.getMessage());

        verify(userRepository).findById(userId);
        verify(userRepository).existsByEmailAndDeletedAtIsNull(testEmail);
        verify(userRepository, never()).restoreUser(any());
        verify(auditService).logEvent(anyString(), anyString(), anyString(), anyString(),
                any(Map.class), any(), anyString(), anyString(), any());
    }

    @Test
    void restoreUser_noAdminAccess_throwsSecurityException() {
        // Given - Clear tenant context to simulate no authentication
        TenantContext.clear();

        // When & Then
        SecurityException exception = assertThrows(SecurityException.class,
                () -> userService.restoreUser(userId));

        assertEquals("Admin access required", exception.getMessage());

        verifyNoInteractions(userRepository, auditService);
    }

    private User createMockUser() {
        User user = new User(new Email(testEmail), testName, testProvider, testProviderId);
        setField(user, "id", userId);
        setField(user, "createdAt", Instant.now());
        setField(user, "updatedAt", Instant.now());
        setField(user, "preferences", testPreferences);
        return user;
    }

    private void setField(Object obj, String fieldName, Object value) {
        try {
            Field field = obj.getClass().getDeclaredField(fieldName);
            field.setAccessible(true);
            field.set(obj, value);
        } catch (Exception e) {
            throw new RuntimeException("Failed to set field " + fieldName, e);
        }
    }
}