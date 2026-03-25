package com.cafeteriamanagement.dto;

import com.cafeteriamanagement.model.enums.Allergen;
import com.cafeteriamanagement.model.enums.IngredientType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

@Schema(description = "Ingredient definition used in recipes and menus")
public class IngredientDTO {

    @Schema(description = "External identifier for the ingredient", example = "550e8400-e29b-41d4-a716-446655440003", accessMode = Schema.AccessMode.READ_ONLY)
    private String id;

    @NotBlank(message = "Name cannot be blank")
    @Pattern(regexp = "^[A-Za-z\\s]+$", message = "Name can only contain letters and spaces")
    @Schema(description = "Ingredient name containing only letters and spaces", example = "Tomato")
    private String name;

    @NotNull(message = "Type is required")
    @Schema(description = "Category of ingredient", example = "VEGETABLE")
    private IngredientType type;

    @NotNull(message = "Allergen is required")
    @Schema(description = "Allergen classification for labeling", example = "NONE")
    private Allergen allergen;

    public IngredientDTO() {}

    public IngredientDTO(String id, String name, IngredientType type, Allergen allergen) {
        this.id = id;
        this.name = name;
        this.type = type;
        this.allergen = allergen;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public IngredientType getType() {
        return type;
    }

    public void setType(IngredientType type) {
        this.type = type;
    }

    public Allergen getAllergen() {
        return allergen;
    }

    public void setAllergen(Allergen allergen) {
        this.allergen = allergen;
    }
}