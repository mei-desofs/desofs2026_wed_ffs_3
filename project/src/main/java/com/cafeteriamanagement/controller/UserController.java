package com.cafeteriamanagement.controller;

import com.cafeteriamanagement.dto.UserDTO;
import com.cafeteriamanagement.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/users")
@CrossOrigin(origins = "*")
@Tag(name = "Users", description = "User management including self-service endpoints")
@SecurityRequirement(name = "bearerAuth")
public class UserController {

    private final UserService userService;

    @Autowired
    public UserController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping
    @Operation(summary = "List users", description = "Retrieve all registered users")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Users retrieved",
            content = @Content(mediaType = "application/json",
                array = @ArraySchema(schema = @Schema(implementation = UserDTO.class))))
    })
    public ResponseEntity<List<UserDTO>> getAllUsers() {
        List<UserDTO> users = userService.getAllUsers();
        return ResponseEntity.ok(users);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Retrieve user", description = "Fetch a user by external identifier")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "User found",
            content = @Content(mediaType = "application/json",
                schema = @Schema(implementation = UserDTO.class))),
        @ApiResponse(responseCode = "404", description = "User not found", content = @Content)
    })
    public ResponseEntity<UserDTO> getUserById(
        @Parameter(description = "External identifier of the user", example = "550e8400-e29b-41d4-a716-446655440040")
        @PathVariable String id) {
        return userService.getUserById(id)
                .map(user -> ResponseEntity.ok(user))
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/by-username/{username}")
    @Operation(summary = "Retrieve user by username", description = "Fetch user details by username")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "User found",
            content = @Content(mediaType = "application/json",
                schema = @Schema(implementation = UserDTO.class))),
        @ApiResponse(responseCode = "404", description = "User not found", content = @Content)
    })
    public ResponseEntity<UserDTO> getUserByUsername(
        @Parameter(description = "Unique username", example = "john_doe")
        @PathVariable String username) {
        return userService.getUserByUsername(username)
                .map(user -> ResponseEntity.ok(user))
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    @Operation(summary = "Create user", description = "Create a new user respecting user type and balance rules")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "201", description = "User created",
            content = @Content(mediaType = "application/json",
                schema = @Schema(implementation = UserDTO.class))),
        @ApiResponse(responseCode = "400", description = "Validation error", content = @Content)
    })
    public ResponseEntity<UserDTO> createUser(
        @io.swagger.v3.oas.annotations.parameters.RequestBody(description = "User payload including credentials, type, and optional balance", required = true,
            content = @Content(schema = @Schema(implementation = UserDTO.class)))
        @Valid @RequestBody UserDTO userDTO) {
        UserDTO createdUser = userService.createUser(userDTO);
        return ResponseEntity.status(HttpStatus.CREATED).body(createdUser);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update user", description = "Update user details including balance adjustments")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "User updated",
            content = @Content(mediaType = "application/json",
                schema = @Schema(implementation = UserDTO.class))),
        @ApiResponse(responseCode = "400", description = "Validation error", content = @Content),
        @ApiResponse(responseCode = "404", description = "User not found", content = @Content)
    })
    public ResponseEntity<UserDTO> updateUser(
        @Parameter(description = "External identifier of the user", example = "550e8400-e29b-41d4-a716-446655440040")
        @PathVariable String id,
        @io.swagger.v3.oas.annotations.parameters.RequestBody(description = "Updated user payload", required = true,
            content = @Content(schema = @Schema(implementation = UserDTO.class)))
        @Valid @RequestBody UserDTO userDTO) {
        return userService.updateUser(id, userDTO)
                .map(user -> ResponseEntity.ok(user))
                .orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete user", description = "Delete a user by external identifier")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "204", description = "User deleted", content = @Content),
        @ApiResponse(responseCode = "404", description = "User not found", content = @Content)
    })
    public ResponseEntity<Void> deleteUser(
        @Parameter(description = "External identifier of the user", example = "550e8400-e29b-41d4-a716-446655440040")
        @PathVariable String id) {
        if (userService.deleteUser(id)) {
            return ResponseEntity.noContent().build();
        } else {
            return ResponseEntity.notFound().build();
        }
    }

    @GetMapping("/me")
    @Operation(summary = "Get current user", description = "Retrieve details of the authenticated user")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "User retrieved",
            content = @Content(mediaType = "application/json",
                schema = @Schema(implementation = UserDTO.class))),
        @ApiResponse(responseCode = "404", description = "User not found", content = @Content)
    })
    public ResponseEntity<UserDTO> getCurrentUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String currentUsername = auth.getName();
        
        return userService.getUserByUsername(currentUsername)
                .map(user -> ResponseEntity.ok(user))
                .orElse(ResponseEntity.notFound().build());
    }

    @PutMapping("/me")
    @Operation(summary = "Update current user", description = "Update profile information for the authenticated user")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "User updated",
            content = @Content(mediaType = "application/json",
                schema = @Schema(implementation = UserDTO.class))),
        @ApiResponse(responseCode = "400", description = "Validation error", content = @Content),
        @ApiResponse(responseCode = "404", description = "User not found", content = @Content)
    })
    public ResponseEntity<UserDTO> updateCurrentUser(
        @io.swagger.v3.oas.annotations.parameters.RequestBody(description = "Updated payload for the authenticated user", required = true,
            content = @Content(schema = @Schema(implementation = UserDTO.class)))
        @Valid @RequestBody UserDTO userDTO) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String currentUsername = auth.getName();
        
        UserDTO currentUser = userService.getUserByUsername(currentUsername).orElse(null);
        if (currentUser == null) {
            return ResponseEntity.notFound().build();
        }
        
        userDTO.setUsername(currentUsername); 
        
        return userService.updateUser(currentUser.getId(), userDTO)
                .map(user -> ResponseEntity.ok(user))
                .orElse(ResponseEntity.notFound().build());
    }
}