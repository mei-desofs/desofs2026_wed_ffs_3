package com.cafeteriamanagement.controller;

import com.cafeteriamanagement.dto.UserDTO;
import com.cafeteriamanagement.model.enums.UserType;
import com.cafeteriamanagement.service.UserService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class UserControllerTest {

    @Mock
    private UserService userService;

    @InjectMocks
    private UserController userController;

    private UserDTO user1;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        user1 = new UserDTO("id1", "john", null, UserType.CLIENT, new BigDecimal("10.00"));
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    private void authenticateAs(String username) {
        Authentication auth = mock(Authentication.class);
        when(auth.getName()).thenReturn(username);
        SecurityContext ctx = SecurityContextHolder.createEmptyContext();
        ctx.setAuthentication(auth);
        SecurityContextHolder.setContext(ctx);
    }

    @Test
    void getAllUsers_ok() {
        when(userService.getAllUsers()).thenReturn(Arrays.asList(user1, user1));
        ResponseEntity<List<UserDTO>> response = userController.getAllUsers();
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(2, response.getBody().size());
    }

    @Test
    void getUserById_found() {
        when(userService.getUserById("id1")).thenReturn(Optional.of(user1));
        assertEquals(HttpStatus.OK, userController.getUserById("id1").getStatusCode());
    }

    @Test
    void getUserById_notFound() {
        when(userService.getUserById("missing")).thenReturn(Optional.empty());
        assertEquals(HttpStatus.NOT_FOUND, userController.getUserById("missing").getStatusCode());
    }

    @Test
    void getUserByUsername_found() {
        when(userService.getUserByUsername("john")).thenReturn(Optional.of(user1));
        assertEquals(HttpStatus.OK, userController.getUserByUsername("john").getStatusCode());
    }

    @Test
    void getUserByUsername_notFound() {
        when(userService.getUserByUsername("ghost")).thenReturn(Optional.empty());
        assertEquals(HttpStatus.NOT_FOUND, userController.getUserByUsername("ghost").getStatusCode());
    }

    @Test
    void createUser_created() {
        when(userService.createUser(any(UserDTO.class))).thenReturn(user1);
        ResponseEntity<UserDTO> response = userController.createUser(user1);
        assertEquals(HttpStatus.CREATED, response.getStatusCode());
    }

    @Test
    void updateUser_found() {
        when(userService.updateUser(eq("id1"), any(UserDTO.class))).thenReturn(Optional.of(user1));
        assertEquals(HttpStatus.OK, userController.updateUser("id1", user1).getStatusCode());
    }

    @Test
    void updateUser_notFound() {
        when(userService.updateUser(eq("missing"), any(UserDTO.class))).thenReturn(Optional.empty());
        assertEquals(HttpStatus.NOT_FOUND, userController.updateUser("missing", user1).getStatusCode());
    }

    @Test
    void deleteUser_success() {
        when(userService.deleteUser("id1")).thenReturn(true);
        assertEquals(HttpStatus.NO_CONTENT, userController.deleteUser("id1").getStatusCode());
    }

    @Test
    void deleteUser_notFound() {
        when(userService.deleteUser("missing")).thenReturn(false);
        assertEquals(HttpStatus.NOT_FOUND, userController.deleteUser("missing").getStatusCode());
    }

    @Test
    void getCurrentUser_found() {
        authenticateAs("john");
        when(userService.getUserByUsername("john")).thenReturn(Optional.of(user1));
        assertEquals(HttpStatus.OK, userController.getCurrentUser().getStatusCode());
    }

    @Test
    void getCurrentUser_notFound() {
        authenticateAs("john");
        when(userService.getUserByUsername("john")).thenReturn(Optional.empty());
        assertEquals(HttpStatus.NOT_FOUND, userController.getCurrentUser().getStatusCode());
    }

    @Test
    void updateCurrentUser_success() {
        authenticateAs("john");
        when(userService.getUserByUsername("john")).thenReturn(Optional.of(user1));
        when(userService.updateUser(eq("id1"), any(UserDTO.class))).thenReturn(Optional.of(user1));
        ResponseEntity<UserDTO> response = userController.updateCurrentUser(
                new UserDTO(null, "ignored", "Sup3rStr0ngP@ss", UserType.CLIENT, BigDecimal.TEN));
        assertEquals(HttpStatus.OK, response.getStatusCode());
    }

    @Test
    void updateCurrentUser_userNotFound() {
        authenticateAs("john");
        when(userService.getUserByUsername("john")).thenReturn(Optional.empty());
        ResponseEntity<UserDTO> response = userController.updateCurrentUser(
                new UserDTO(null, "ignored", "Sup3rStr0ngP@ss", UserType.CLIENT, BigDecimal.TEN));
        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
    }

    @Test
    void updateCurrentUser_ignoresClientControlledTypeAndBalance() {
        authenticateAs("john");
        when(userService.getUserByUsername("john")).thenReturn(Optional.of(user1));
        when(userService.updateUser(eq("id1"), any(UserDTO.class))).thenReturn(Optional.of(user1));

        UserDTO payload =
                new UserDTO(null, "ignored", "Sup3rStr0ngP@ss", UserType.ADMIN, new BigDecimal("99999.00"));

        userController.updateCurrentUser(payload);

        ArgumentCaptor<UserDTO> captor = ArgumentCaptor.forClass(UserDTO.class);
        verify(userService).updateUser(eq("id1"), captor.capture());
        UserDTO forwarded = captor.getValue();

        assertEquals("john", forwarded.getUsername());
        assertEquals(UserType.CLIENT, forwarded.getType());
        assertEquals(new BigDecimal("10.00"), forwarded.getBalance());
    }
}
