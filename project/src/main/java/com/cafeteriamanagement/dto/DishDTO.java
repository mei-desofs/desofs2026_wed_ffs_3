package com.cafeteriamanagement.dto;

import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

import java.math.BigDecimal;
import java.util.List;

@Schema(description = "Dish representation exposed via API")
public class DishDTO {

    @Schema(description = "External identifier for the dish", example = "550e8400-e29b-41d4-a716-446655440010", accessMode = Schema.AccessMode.READ_ONLY)
    private String id;

    @NotBlank(message = "Name cannot be blank")
    @Pattern(regexp = "^[A-Za-z\\s]+$", message = "Name can only contain letters and spaces")
    @Schema(description = "Name of the dish", example = "Veggie Sandwich")
    private String name;

    @NotEmpty(message = "Ingredients list cannot be empty")
    @ArraySchema(schema = @Schema(description = "Ingredient name associated with this dish", example = "Tomato"))
    private List<String> ingredientNames;

    @NotNull(message = "Price is required")
    @DecimalMin(value = "0.0", inclusive = false, message = "Price must be greater than 0")
    @Schema(description = "Price in euros", example = "7.99")
    private BigDecimal price;

    public DishDTO() {}

    public DishDTO(String id, String name, List<String> ingredientNames, BigDecimal price) {
        this.id = id;
        this.name = name;
        this.ingredientNames = ingredientNames;
        this.price = price;
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

    public List<String> getIngredientNames() {
        return ingredientNames;
    }

    public void setIngredientNames(List<String> ingredientNames) {
        this.ingredientNames = ingredientNames;
    }

    public BigDecimal getPrice() {
        return price;
    }

    public void setPrice(BigDecimal price) {
        this.price = price;
    }
}