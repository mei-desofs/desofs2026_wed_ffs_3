package com.cafeteriamanagement.service;

import com.cafeteriamanagement.dto.UserDTO;
import com.cafeteriamanagement.model.entity.User;
import com.cafeteriamanagement.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@Transactional
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Autowired
    public UserService(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    public List<UserDTO> getAllUsers() {
        return userRepository.findAll().stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    public Optional<UserDTO> getUserById(String externalId) {
        return userRepository.findByExternalId(externalId)
                .map(this::convertToDTO);
    }

    public Optional<UserDTO> getUserByUsername(String username) {
        return userRepository.findByUsername(username)
                .map(this::convertToDTO);
    }

    public UserDTO createUser(UserDTO userDTO) {
        if (userRepository.existsByUsername(userDTO.getUsername())) {
            throw new IllegalArgumentException("Username already exists: " + userDTO.getUsername());
        }
        User user = convertToEntity(userDTO);
        User savedUser = userRepository.save(user);
        return convertToDTO(savedUser);
    }

    public Optional<UserDTO> updateUser(String externalId, UserDTO userDTO) {
        return userRepository.findByExternalId(externalId)
                .map(user -> {
                    if (!user.getUsername().equals(userDTO.getUsername()) && 
                        userRepository.existsByUsername(userDTO.getUsername())) {
                        throw new IllegalArgumentException("Username already exists: " + userDTO.getUsername());
                    }
                    String encryptedPassword = passwordEncoder.encode(userDTO.getPassword());
                    user.updateDetails(userDTO.getUsername(), encryptedPassword, userDTO.getType(), userDTO.getBalance());
                    User savedUser = userRepository.save(user);
                    return convertToDTO(savedUser);
                });
    }

    public boolean deleteUser(String externalId) {
        return userRepository.findByExternalId(externalId)
                .map(user -> {
                    userRepository.delete(user);
                    return true;
                })
                .orElse(false);
    }

    public User findByExternalId(String externalId) {
        return userRepository.findByExternalId(externalId)
                .orElseThrow(() -> new IllegalArgumentException("User not found with id: " + externalId));
    }

    public User findByUsername(String username) {
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new IllegalArgumentException("User not found with username: " + username));
    }

    public UserDTO convertToDTO(User user) {
    UserDTO dto = new UserDTO();
    dto.setId(user.getExternalId());
    dto.setUsername(user.getUsername());
    dto.setType(user.getType());
    dto.setBalance(user.getBalance());
    return dto;
    }

    private User convertToEntity(UserDTO dto) {
    String encryptedPassword = passwordEncoder.encode(dto.getPassword());
    return new User(dto.getUsername(), encryptedPassword, dto.getType(), dto.getBalance());
    }
}