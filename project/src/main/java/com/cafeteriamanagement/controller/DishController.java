package com.cafeteriamanagement.controller;

import com.cafeteriamanagement.dto.DishDTO;
import com.cafeteriamanagement.service.DishService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/dishes")
@CrossOrigin(origins = "*")
@Tag(name = "Dishes", description = "Manage dishes served by the cafeteria")
@SecurityRequirement(name = "bearerAuth")
public class DishController {

    private final DishService dishService;

    @Autowired
    public DishController(DishService dishService) {
        this.dishService = dishService;
    }

    @GetMapping
    @Operation(summary = "List dishes", description = "Retrieve all dishes with ingredient references")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Dishes retrieved",
            content = @Content(mediaType = "application/json",
                array = @ArraySchema(schema = @Schema(implementation = DishDTO.class))))
    })
    public ResponseEntity<List<DishDTO>> getAllDishes() {
        List<DishDTO> dishes = dishService.getAllDishes();
        return ResponseEntity.ok(dishes);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Retrieve dish", description = "Fetch a dish by its external identifier")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Dish found",
            content = @Content(mediaType = "application/json",
                schema = @Schema(implementation = DishDTO.class))),
        @ApiResponse(responseCode = "404", description = "Dish not found", content = @Content)
    })
    public ResponseEntity<DishDTO> getDishById(
        @Parameter(description = "External identifier of the dish", example = "550e8400-e29b-41d4-a716-446655440010")
        @PathVariable String id) {
        return dishService.getDishById(id)
                .map(dish -> ResponseEntity.ok(dish))
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    @Operation(summary = "Create dish", description = "Register a new dish with validated name, ingredients, and price")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "201", description = "Dish created",
            content = @Content(mediaType = "application/json",
                schema = @Schema(implementation = DishDTO.class))),
        @ApiResponse(responseCode = "400", description = "Validation error", content = @Content)
    })
    public ResponseEntity<DishDTO> createDish(
        @io.swagger.v3.oas.annotations.parameters.RequestBody(description = "Dish payload describing name, ingredient references, and price", required = true,
            content = @Content(schema = @Schema(implementation = DishDTO.class)))
        @Valid @RequestBody DishDTO dishDTO) {
        DishDTO createdDish = dishService.createDish(dishDTO);
        return ResponseEntity.status(HttpStatus.CREATED).body(createdDish);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update dish", description = "Modify an existing dish by its external identifier")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Dish updated",
            content = @Content(mediaType = "application/json",
                schema = @Schema(implementation = DishDTO.class))),
        @ApiResponse(responseCode = "400", description = "Validation error", content = @Content),
        @ApiResponse(responseCode = "404", description = "Dish not found", content = @Content)
    })
    public ResponseEntity<DishDTO> updateDish(
        @Parameter(description = "External identifier of the dish", example = "550e8400-e29b-41d4-a716-446655440010")
        @PathVariable String id,
        @io.swagger.v3.oas.annotations.parameters.RequestBody(description = "Updated dish payload", required = true,
            content = @Content(schema = @Schema(implementation = DishDTO.class)))
        @Valid @RequestBody DishDTO dishDTO) {
        return dishService.updateDish(id, dishDTO)
                .map(dish -> ResponseEntity.ok(dish))
                .orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete dish", description = "Remove a dish by its external identifier")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "204", description = "Dish deleted", content = @Content),
        @ApiResponse(responseCode = "404", description = "Dish not found", content = @Content)
    })
    public ResponseEntity<Void> deleteDish(
        @Parameter(description = "External identifier of the dish", example = "550e8400-e29b-41d4-a716-446655440010")
        @PathVariable String id) {
        if (dishService.deleteDish(id)) {
            return ResponseEntity.noContent().build();
        } else {
            return ResponseEntity.notFound().build();
        }
    }
}