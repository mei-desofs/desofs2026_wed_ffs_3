package com.cafeteriamanagement.service;
import com.cafeteriamanagement.model.entity.User;
import com.cafeteriamanagement.model.enums.UserType;
import com.cafeteriamanagement.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import java.math.BigDecimal;
import java.util.Optional;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

public class UserServiceTest {
    @Mock
    private UserRepository userRepository;
    @InjectMocks
    private UserService userService;
    @BeforeEach
    void setUp() {
    MockitoAnnotations.openMocks(this);
    }
    @Test
    void testFindByExternalId_UserExists() {
        User user = new User("john", "pass", UserType.CLIENT, new BigDecimal("10.00"));
        when(userRepository.findByExternalId("id123")).thenReturn(Optional.of(user));
        User result = userService.findByExternalId("id123");
        assertNotNull(result);
        assertEquals("john", result.getUsername());
    }
    @Test
    void testFindByExternalId_UserNotFound() {
        when(userRepository.findByExternalId("id999")).thenReturn(Optional.empty());
        assertThrows(IllegalArgumentException.class, () -> userService.findByExternalId("id999"));
    }

    @Test
    void testFindByUsername_UserExists() {
        User user = new User("alice", "pass2", UserType.ADMIN, new BigDecimal("20.00"));
        when(userRepository.findByUsername("alice")).thenReturn(Optional.of(user));
        User result = userService.findByUsername("alice");
        assertNotNull(result);
        assertEquals("alice", result.getUsername());
    }
}