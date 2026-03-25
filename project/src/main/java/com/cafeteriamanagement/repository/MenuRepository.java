package com.cafeteriamanagement.repository;

import com.cafeteriamanagement.model.entity.Menu;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.Optional;

@Repository
public interface MenuRepository extends JpaRepository<Menu, Long> {
    
    Optional<Menu> findByExternalId(String externalId);
    
    Optional<Menu> findByDate(LocalDate date);
    
    boolean existsByExternalId(String externalId);
    
    boolean existsByDate(LocalDate date);
}