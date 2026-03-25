package com.cafeteriamanagement.repository;

import com.cafeteriamanagement.model.entity.Dish;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface DishRepository extends JpaRepository<Dish, Long> {
    
    Optional<Dish> findByExternalId(String externalId);
    
    Optional<Dish> findByNameValue(String name);
    
    boolean existsByExternalId(String externalId);
}