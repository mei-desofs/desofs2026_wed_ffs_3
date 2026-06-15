package com.cafeteriamanagement.service;

import com.cafeteriamanagement.dto.UserDTO;
import com.cafeteriamanagement.model.entity.User;
import com.cafeteriamanagement.model.enums.UserType;
import com.cafeteriamanagement.repository.UserRepository;
import com.cafeteriamanagement.security.HaveIBeenPwnedClient;
import com.cafeteriamanagement.security.PasswordPolicyService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class UserServiceTest {

    @Mock
    private UserRepository userRepository;
    @Mock
    private PasswordEncoder passwordEncoder;
    @Mock
    private PasswordPolicyService passwordPolicyService;
    @Mock
    private HaveIBeenPwnedClient hibpClient;

    @InjectMocks
    private UserService userService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    private UserDTO clientDTO() {
        return new UserDTO(null, "john", "Sup3rStr0ngP@ss", UserType.CLIENT, new BigDecimal("10.00"));
    }

    // ---- read ----

    @Test
    void getAllUsers_returnsAll() {
        when(userRepository.findAll()).thenReturn(Arrays.asList(
                new User("john", "p", UserType.CLIENT, new BigDecimal("10.00")),
                new User("admin", "p", UserType.ADMIN, new BigDecimal("0.00"))));
        List<UserDTO> result = userService.getAllUsers();
        assertEquals(2, result.size());
    }

    @Test
    void getUserById_present() {
        User user = new User("john", "p", UserType.CLIENT, new BigDecimal("10.00"));
        when(userRepository.findByExternalId("id1")).thenReturn(Optional.of(user));
        assertTrue(userService.getUserById("id1").isPresent());
    }

    @Test
    void getUserById_notFound() {
        when(userRepository.findByExternalId("missing")).thenReturn(Optional.empty());
        assertFalse(userService.getUserById("missing").isPresent());
    }

    @Test
    void getUserByUsername_present() {
        when(userRepository.findByUsername("john"))
                .thenReturn(Optional.of(new User("john", "p", UserType.CLIENT, BigDecimal.TEN)));
        assertTrue(userService.getUserByUsername("john").isPresent());
    }

    @Test
    void getUserByUsername_notFound() {
        when(userRepository.findByUsername("ghost")).thenReturn(Optional.empty());
        assertFalse(userService.getUserByUsername("ghost").isPresent());
    }

    // ---- create ----

    @Test
    void createUser_success() {
        UserDTO dto = clientDTO();
        when(userRepository.existsByUsername("john")).thenReturn(false);
        when(hibpClient.isBreached(anyString())).thenReturn(false);
        when(passwordEncoder.encode("Sup3rStr0ngP@ss")).thenReturn("hashed");
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

        UserDTO result = userService.createUser(dto);

        assertEquals("john", result.getUsername());
        assertEquals(UserType.CLIENT, result.getType());
        verify(passwordPolicyService).validate("Sup3rStr0ngP@ss");
        verify(passwordEncoder).encode("Sup3rStr0ngP@ss");
        verify(userRepository).save(any(User.class));
    }

    @Test
    void createUser_usernameExists_throws() {
        when(userRepository.existsByUsername("john")).thenReturn(true);
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> userService.createUser(clientDTO()));
        assertTrue(ex.getMessage().contains("already exists"));
        verify(userRepository, never()).save(any());
    }

    @Test
    void createUser_breachedPassword_throws() {
        when(userRepository.existsByUsername("john")).thenReturn(false);
        when(hibpClient.isBreached(anyString())).thenReturn(true);
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> userService.createUser(clientDTO()));
        assertTrue(ex.getMessage().contains("data breaches"));
    }

    @Test
    void createUser_policyRejects_propagates() {
        when(userRepository.existsByUsername("john")).thenReturn(false);
        doThrow(new IllegalArgumentException("Password too short"))
                .when(passwordPolicyService).validate(anyString());
        assertThrows(IllegalArgumentException.class, () -> userService.createUser(clientDTO()));
    }

    // ---- update ----

    @Test
    void updateUser_success() {
        User existing = new User("john", "p", UserType.CLIENT, new BigDecimal("10.00"));
        when(userRepository.findByExternalId("id1")).thenReturn(Optional.of(existing));
        when(hibpClient.isBreached(anyString())).thenReturn(false);
        when(passwordEncoder.encode(anyString())).thenReturn("hashed");
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

        UserDTO dto = new UserDTO(null, "john", "An0therStr0ngP@", UserType.CLIENT, new BigDecimal("5.00"));
        Optional<UserDTO> result = userService.updateUser("id1", dto);

        assertTrue(result.isPresent());
        verify(userRepository).save(existing);
    }

    @Test
    void updateUser_notFound() {
        when(userRepository.findByExternalId("missing")).thenReturn(Optional.empty());
        assertFalse(userService.updateUser("missing", clientDTO()).isPresent());
    }

    @Test
    void updateUser_usernameConflict_throws() {
        User existing = new User("john", "p", UserType.CLIENT, new BigDecimal("10.00"));
        when(userRepository.findByExternalId("id1")).thenReturn(Optional.of(existing));
        when(userRepository.existsByUsername("taken")).thenReturn(true);
        UserDTO dto = new UserDTO(null, "taken", "An0therStr0ngP@", UserType.CLIENT, BigDecimal.TEN);
        assertThrows(IllegalArgumentException.class, () -> userService.updateUser("id1", dto));
    }

    @Test
    void updateUser_breachedPassword_throws() {
        User existing = new User("john", "p", UserType.CLIENT, new BigDecimal("10.00"));
        when(userRepository.findByExternalId("id1")).thenReturn(Optional.of(existing));
        when(hibpClient.isBreached(anyString())).thenReturn(true);
        assertThrows(IllegalArgumentException.class,
                () -> userService.updateUser("id1", clientDTO()));
    }

    // ---- delete ----

    @Test
    void deleteUser_success() {
        User user = new User("john", "p", UserType.CLIENT, BigDecimal.TEN);
        when(userRepository.findByExternalId("id1")).thenReturn(Optional.of(user));
        assertTrue(userService.deleteUser("id1"));
        verify(userRepository).delete(user);
    }

    @Test
    void deleteUser_notFound() {
        when(userRepository.findByExternalId("missing")).thenReturn(Optional.empty());
        assertFalse(userService.deleteUser("missing"));
    }

    // ---- lookups ----

    @Test
    void findByExternalId_exists() {
        User user = new User("john", "pass", UserType.CLIENT, new BigDecimal("10.00"));
        when(userRepository.findByExternalId("id123")).thenReturn(Optional.of(user));
        assertEquals("john", userService.findByExternalId("id123").getUsername());
    }

    @Test
    void findByExternalId_notFound_throws() {
        when(userRepository.findByExternalId("id999")).thenReturn(Optional.empty());
        assertThrows(IllegalArgumentException.class, () -> userService.findByExternalId("id999"));
    }

    @Test
    void findByUsername_exists() {
        User user = new User("alice", "pass2", UserType.ADMIN, new BigDecimal("20.00"));
        when(userRepository.findByUsername("alice")).thenReturn(Optional.of(user));
        assertEquals("alice", userService.findByUsername("alice").getUsername());
    }

    @Test
    void findByUsername_notFound_throws() {
        when(userRepository.findByUsername("ghost")).thenReturn(Optional.empty());
        assertThrows(IllegalArgumentException.class, () -> userService.findByUsername("ghost"));
    }

    @Test
    void convertToDTO_mapsFieldsAndOmitsPassword() {
        User user = new User("john", "secret-hash", UserType.CLIENT, new BigDecimal("12.50"));
        UserDTO dto = userService.convertToDTO(user);
        assertEquals("john", dto.getUsername());
        assertEquals(UserType.CLIENT, dto.getType());
        assertEquals(new BigDecimal("12.50"), dto.getBalance());
        assertNull(dto.getPassword(), "DTO must never carry the password back to clients");
    }
}
