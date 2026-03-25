package com.cafeteriamanagement.repository;

import com.cafeteriamanagement.model.entity.Ingredient;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface IngredientRepository extends JpaRepository<Ingredient, Long> {
    
    Optional<Ingredient> findByExternalId(String externalId);
    
    Optional<Ingredient> findByNameValue(String name);
    
    boolean existsByExternalId(String externalId);
}