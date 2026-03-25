package com.cafeteriamanagement.controller;

import com.cafeteriamanagement.dto.UserDTO;
import com.cafeteriamanagement.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.http.ResponseEntity;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

public class UserControllerTest {
    @Mock
    private UserService userService;

    @InjectMocks
    private UserController userController;

    private UserDTO user1;
    private UserDTO user2;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        user1 = new UserDTO();
        user1.setId("id1");
        user1.setUsername("user1");
        user2 = new UserDTO();
        user2.setId("id2");
        user2.setUsername("user2");
    }

    @Test
    void testGetAllUsers() {
        when(userService.getAllUsers()).thenReturn(Arrays.asList(user1, user2));
        ResponseEntity<List<UserDTO>> response = userController.getAllUsers();
        assertTrue(response.getStatusCode().is2xxSuccessful());
        List<UserDTO> result = response.getBody();
        assertEquals(2, result.size());
    }

    @Test
    void testGetUserById() {
        when(userService.getUserById("id1")).thenReturn(Optional.of(user1));
        ResponseEntity<UserDTO> response = userController.getUserById("id1");
        assertTrue(response.getStatusCode().is2xxSuccessful());
        assertEquals("user1", response.getBody().getUsername());
    }

    @Test
    void testGetUserById_NotFound() {
        when(userService.getUserById("missing")).thenReturn(Optional.empty());
        ResponseEntity<UserDTO> response = userController.getUserById("missing");
        assertEquals(404, response.getStatusCodeValue());
    }
}
