package com.cafeteriamanagement.service;

import com.cafeteriamanagement.dto.DishDTO;
import com.cafeteriamanagement.model.entity.Dish;
import com.cafeteriamanagement.model.entity.Ingredient;
import com.cafeteriamanagement.model.valueobject.Name;
import com.cafeteriamanagement.repository.DishRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@Transactional
public class DishService {

    private final DishRepository dishRepository;
    private final IngredientService ingredientService;

    @Autowired
    public DishService(DishRepository dishRepository, IngredientService ingredientService) {
        this.dishRepository = dishRepository;
        this.ingredientService = ingredientService;
    }

    public List<DishDTO> getAllDishes() {
        return dishRepository.findAll().stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    public Optional<DishDTO> getDishById(String externalId) {
        return dishRepository.findByExternalId(externalId)
                .map(this::convertToDTO);
    }

    public DishDTO createDish(DishDTO dishDTO) {
        Dish dish = convertToEntity(dishDTO);
        Dish savedDish = dishRepository.save(dish);
        return convertToDTO(savedDish);
    }

    public Optional<DishDTO> updateDish(String externalId, DishDTO dishDTO) {
        return dishRepository.findByExternalId(externalId)
                .map(dish -> {
                    List<Ingredient> ingredients = dishDTO.getIngredientNames().stream()
                            .map(ingredientService::findByName)
                            .collect(Collectors.toList());
                    
                    dish.updateDetails(
                            new Name(dishDTO.getName()),
                            ingredients,
                            dishDTO.getPrice()
                    );
                    Dish savedDish = dishRepository.save(dish);
                    return convertToDTO(savedDish);
                });
    }

    public boolean deleteDish(String externalId) {
        return dishRepository.findByExternalId(externalId)
                .map(dish -> {
                    dishRepository.delete(dish);
                    return true;
                })
                .orElse(false);
    }

    public Dish findByExternalId(String externalId) {
        return dishRepository.findByExternalId(externalId)
                .orElseThrow(() -> new IllegalArgumentException("Dish not found with id: " + externalId));
    }

    public Dish findByName(String name) {
        return dishRepository.findByNameValue(name)
                .orElseThrow(() -> new IllegalArgumentException("Dish not found with name: " + name));
    }

    private DishDTO convertToDTO(Dish dish) {
        DishDTO dto = new DishDTO();
        dto.setId(dish.getExternalId());
        dto.setName(dish.getName().getValue());
        dto.setIngredientNames(dish.getIngredients().stream()
                .map(ingredient -> ingredient.getName().getValue())
                .collect(Collectors.toList()));
        dto.setPrice(dish.getPrice());
        return dto;
    }

    private Dish convertToEntity(DishDTO dto) {
        List<String> missingIngredients = new ArrayList<>();
        List<Ingredient> ingredients = new ArrayList<>();
        
        for (String ingredientName : dto.getIngredientNames()) {
            try {
                Ingredient ingredient = ingredientService.findByName(ingredientName);
                ingredients.add(ingredient);
            } catch (IllegalArgumentException e) {
                missingIngredients.add(ingredientName);
            }
        }
        
        if (!missingIngredients.isEmpty()) {
            String errorMessage = missingIngredients.size() == 1 
                ? "Ingredient not found: " + missingIngredients.get(0)
                : "Ingredients not found: " + String.join(", ", missingIngredients);
            throw new IllegalArgumentException(errorMessage);
        }
        
        return new Dish(
                new Name(dto.getName()),
                ingredients,
                dto.getPrice()
        );
    }
}