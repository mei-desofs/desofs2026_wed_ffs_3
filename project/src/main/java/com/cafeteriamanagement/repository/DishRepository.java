package com.cafeteriamanagement.repository;

import com.cafeteriamanagement.model.entity.Dish;
import com.cafeteriamanagement.model.enums.Allergen;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface DishRepository extends JpaRepository<Dish, Long> {

    Optional<Dish> findByExternalId(String externalId);

    Optional<Dish> findByNameValue(String name);

    boolean existsByExternalId(String externalId);

    @Query("SELECT DISTINCT d FROM Dish d JOIN d.ingredients i WHERE i.allergen = :allergen")
    List<Dish> findByIngredientsAllergen(@Param("allergen") Allergen allergen);
}