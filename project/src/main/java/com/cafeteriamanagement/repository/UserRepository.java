package com.cafeteriamanagement.repository;

import com.cafeteriamanagement.model.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    
    Optional<User> findByExternalId(String externalId);
    
    Optional<User> findByUsername(String username);
    
    boolean existsByExternalId(String externalId);
    
    boolean existsByUsername(String username);
}