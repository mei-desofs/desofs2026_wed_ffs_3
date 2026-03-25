package com.cafeteriamanagement.controller;

import com.cafeteriamanagement.dto.IngredientDTO;
import com.cafeteriamanagement.service.IngredientService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.Parameter;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/ingredients")
@CrossOrigin(origins = "*")
@Tag(name = "Ingredients", description = "CRUD operations for managing available ingredients")
@SecurityRequirement(name = "bearerAuth")
public class IngredientController {

    private final IngredientService ingredientService;

    @Autowired
    public IngredientController(IngredientService ingredientService) {
        this.ingredientService = ingredientService;
    }

    @GetMapping
    @Operation(summary = "List ingredients", description = "Retrieve all registered ingredients")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Ingredients fetched successfully",
            content = @Content(mediaType = "application/json",
                array = @ArraySchema(schema = @Schema(implementation = IngredientDTO.class))))
    })
    public ResponseEntity<List<IngredientDTO>> getAllIngredients() {
        List<IngredientDTO> ingredients = ingredientService.getAllIngredients();
        return ResponseEntity.ok(ingredients);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Retrieve ingredient", description = "Fetch a single ingredient by its external identifier")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Ingredient found",
            content = @Content(mediaType = "application/json",
                schema = @Schema(implementation = IngredientDTO.class))),
        @ApiResponse(responseCode = "404", description = "Ingredient not found", content = @Content)
    })
    public ResponseEntity<IngredientDTO> getIngredientById(
        @Parameter(description = "External identifier of the ingredient", example = "550e8400-e29b-41d4-a716-446655440003")
        @PathVariable String id) {
        return ingredientService.getIngredientById(id)
                .map(ingredient -> ResponseEntity.ok(ingredient))
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    @Operation(summary = "Create ingredient", description = "Register a new ingredient with validation on name, type, and allergen")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "201", description = "Ingredient created",
            content = @Content(mediaType = "application/json",
                schema = @Schema(implementation = IngredientDTO.class))),
        @ApiResponse(responseCode = "400", description = "Validation failure", content = @Content)
    })
    public ResponseEntity<IngredientDTO> createIngredient(
        @io.swagger.v3.oas.annotations.parameters.RequestBody(description = "Ingredient details", required = true,
            content = @Content(schema = @Schema(implementation = IngredientDTO.class)))
        @Valid @RequestBody IngredientDTO ingredientDTO) {
        IngredientDTO createdIngredient = ingredientService.createIngredient(ingredientDTO);
        return ResponseEntity.status(HttpStatus.CREATED).body(createdIngredient);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update ingredient", description = "Modify an existing ingredient by external identifier")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Ingredient updated",
            content = @Content(mediaType = "application/json",
                schema = @Schema(implementation = IngredientDTO.class))),
        @ApiResponse(responseCode = "400", description = "Validation failure", content = @Content),
        @ApiResponse(responseCode = "404", description = "Ingredient not found", content = @Content)
    })
    public ResponseEntity<IngredientDTO> updateIngredient(
        @Parameter(description = "External identifier of the ingredient", example = "550e8400-e29b-41d4-a716-446655440003")
        @PathVariable String id,
        @io.swagger.v3.oas.annotations.parameters.RequestBody(description = "Updated ingredient details", required = true,
            content = @Content(schema = @Schema(implementation = IngredientDTO.class)))
        @Valid @RequestBody IngredientDTO ingredientDTO) {
        return ingredientService.updateIngredient(id, ingredientDTO)
                .map(ingredient -> ResponseEntity.ok(ingredient))
                .orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete ingredient", description = "Remove an ingredient by external identifier")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "204", description = "Ingredient deleted", content = @Content),
        @ApiResponse(responseCode = "404", description = "Ingredient not found", content = @Content)
    })
    public ResponseEntity<Void> deleteIngredient(
        @Parameter(description = "External identifier of the ingredient", example = "550e8400-e29b-41d4-a716-446655440003")
        @PathVariable String id) {
        if (ingredientService.deleteIngredient(id)) {
            return ResponseEntity.noContent().build();
        } else {
            return ResponseEntity.notFound().build();
        }
    }
}