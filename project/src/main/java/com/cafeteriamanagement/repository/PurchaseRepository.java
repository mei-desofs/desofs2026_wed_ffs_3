package com.cafeteriamanagement.repository;

import com.cafeteriamanagement.model.entity.Purchase;
import com.cafeteriamanagement.model.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface PurchaseRepository extends JpaRepository<Purchase, Long> {
    
    Optional<Purchase> findByExternalId(String externalId);
    
    List<Purchase> findByClient(User client);
    
    List<Purchase> findByDate(LocalDate date);
    
    List<Purchase> findByClientAndDate(User client, LocalDate date);
    
    boolean existsByExternalId(String externalId);
}