package com.cafeteriamanagement.service;

import com.cafeteriamanagement.dto.IngredientDTO;
import com.cafeteriamanagement.model.entity.Ingredient;
import com.cafeteriamanagement.model.valueobject.Name;
import com.cafeteriamanagement.repository.IngredientRepository;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@Transactional
public class IngredientService {

    private final IngredientRepository ingredientRepository;
    private final ModelMapper modelMapper;

    @Autowired
    public IngredientService(IngredientRepository ingredientRepository, ModelMapper modelMapper) {
        this.ingredientRepository = ingredientRepository;
        this.modelMapper = modelMapper;
    }

    public List<IngredientDTO> getAllIngredients() {
        return ingredientRepository.findAll().stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    public Optional<IngredientDTO> getIngredientById(String externalId) {
        return ingredientRepository.findByExternalId(externalId)
                .map(this::convertToDTO);
    }

    public IngredientDTO createIngredient(IngredientDTO ingredientDTO) {
        Ingredient ingredient = convertToEntity(ingredientDTO);
        Ingredient savedIngredient = ingredientRepository.save(ingredient);
        return convertToDTO(savedIngredient);
    }

    public Optional<IngredientDTO> updateIngredient(String externalId, IngredientDTO ingredientDTO) {
        return ingredientRepository.findByExternalId(externalId)
                .map(ingredient -> {
                    ingredient.updateDetails(
                            new Name(ingredientDTO.getName()),
                            ingredientDTO.getType(),
                            ingredientDTO.getAllergen()
                    );
                    Ingredient savedIngredient = ingredientRepository.save(ingredient);
                    return convertToDTO(savedIngredient);
                });
    }

    public boolean deleteIngredient(String externalId) {
        return ingredientRepository.findByExternalId(externalId)
                .map(ingredient -> {
                    ingredientRepository.delete(ingredient);
                    return true;
                })
                .orElse(false);
    }

    public Ingredient findByExternalId(String externalId) {
        return ingredientRepository.findByExternalId(externalId)
                .orElseThrow(() -> new IllegalArgumentException("Ingredient not found with id: " + externalId));
    }

    public Ingredient findByName(String name) {
        return ingredientRepository.findByNameValue(name)
                .orElseThrow(() -> new IllegalArgumentException("Ingredient not found with name: " + name));
    }

    private IngredientDTO convertToDTO(Ingredient ingredient) {
        IngredientDTO dto = new IngredientDTO();
        dto.setId(ingredient.getExternalId());
        dto.setName(ingredient.getName().getValue());
        dto.setType(ingredient.getType());
        dto.setAllergen(ingredient.getAllergen());
        return dto;
    }

    private Ingredient convertToEntity(IngredientDTO dto) {
        return new Ingredient(
                new Name(dto.getName()),
                dto.getType(),
                dto.getAllergen()
        );
    }
}