package com.cafeteriamanagement.controller;

import com.cafeteriamanagement.dto.MenuDTO;
import com.cafeteriamanagement.service.MenuService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/menus")
@CrossOrigin(origins = "*")
@Tag(name = "Menus", description = "Manage daily cafeteria menus")
@SecurityRequirement(name = "bearerAuth")
public class MenuController {

    private final MenuService menuService;

    @Autowired
    public MenuController(MenuService menuService) {
        this.menuService = menuService;
    }

    @GetMapping
    @Operation(summary = "List menus", description = "Retrieve all menus sorted by date")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Menus retrieved",
            content = @Content(mediaType = "application/json",
                array = @ArraySchema(schema = @Schema(implementation = MenuDTO.class))))
    })
    public ResponseEntity<List<MenuDTO>> getAllMenus() {
        List<MenuDTO> menus = menuService.getAllMenus();
        return ResponseEntity.ok(menus);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Retrieve menu", description = "Fetch a menu by its external identifier")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Menu found",
            content = @Content(mediaType = "application/json",
                schema = @Schema(implementation = MenuDTO.class))),
        @ApiResponse(responseCode = "404", description = "Menu not found", content = @Content)
    })
    public ResponseEntity<MenuDTO> getMenuById(
        @Parameter(description = "External identifier of the menu", example = "550e8400-e29b-41d4-a716-446655440020")
        @PathVariable String id) {
        return menuService.getMenuById(id)
                .map(menu -> ResponseEntity.ok(menu))
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/by-date/{date}")
    @Operation(summary = "Retrieve menu by date", description = "Fetch a menu based on its scheduled date")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Menu found",
            content = @Content(mediaType = "application/json",
                schema = @Schema(implementation = MenuDTO.class))),
        @ApiResponse(responseCode = "404", description = "Menu not found", content = @Content)
    })
    public ResponseEntity<MenuDTO> getMenuByDate(
        @Parameter(description = "Date in ISO format (YYYY-MM-DD)", example = "2024-06-01")
        @PathVariable String date) {
        LocalDate menuDate = LocalDate.parse(date);
        return menuService.getMenuByDate(menuDate)
                .map(menu -> ResponseEntity.ok(menu))
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN') or (hasRole('EMPLOYEE') and @menuService.isFutureDate(#menuDTO.date))")
    @Operation(summary = "Create menu", description = "Create a new menu for a future date with optional dish assignments")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "201", description = "Menu created",
            content = @Content(mediaType = "application/json",
                schema = @Schema(implementation = MenuDTO.class))),
        @ApiResponse(responseCode = "400", description = "Validation error", content = @Content),
        @ApiResponse(responseCode = "403", description = "Forbidden", content = @Content)
    })
    public ResponseEntity<MenuDTO> createMenu(
        @io.swagger.v3.oas.annotations.parameters.RequestBody(description = "Menu payload containing scheduled date and optional dish names", required = true,
            content = @Content(schema = @Schema(implementation = MenuDTO.class)))
        @Valid @RequestBody MenuDTO menuDTO) {
        if (menuDTO.getDate() != null && !menuDTO.getDate().isAfter(LocalDate.now())) {
            throw new IllegalArgumentException("Menus can only be created for future dates");
        }
        MenuDTO createdMenu = menuService.createMenu(menuDTO);
        return ResponseEntity.status(HttpStatus.CREATED).body(createdMenu);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update menu", description = "Update menu details for a future date")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Menu updated",
            content = @Content(mediaType = "application/json",
                schema = @Schema(implementation = MenuDTO.class))),
        @ApiResponse(responseCode = "400", description = "Validation error", content = @Content),
        @ApiResponse(responseCode = "404", description = "Menu not found", content = @Content)
    })
    public ResponseEntity<MenuDTO> updateMenu(
        @Parameter(description = "External identifier of the menu", example = "550e8400-e29b-41d4-a716-446655440020")
        @PathVariable String id,
        @io.swagger.v3.oas.annotations.parameters.RequestBody(description = "Menu payload to update with future date and dish selections", required = true,
            content = @Content(schema = @Schema(implementation = MenuDTO.class)))
        @Valid @RequestBody MenuDTO menuDTO) {
        if (menuDTO.getDate() != null && !menuDTO.getDate().isAfter(LocalDate.now())) {
            throw new IllegalArgumentException("Menus can only be edited for future dates");
        }
        return menuService.updateMenu(id, menuDTO)
                .map(menu -> ResponseEntity.ok(menu))
                .orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete menu", description = "Delete a menu scheduled for a future date")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "204", description = "Menu deleted", content = @Content),
        @ApiResponse(responseCode = "400", description = "Cannot delete past or current menu", content = @Content),
        @ApiResponse(responseCode = "404", description = "Menu not found", content = @Content)
    })
    public ResponseEntity<Void> deleteMenu(
        @Parameter(description = "External identifier of the menu", example = "550e8400-e29b-41d4-a716-446655440020")
        @PathVariable String id) {
        MenuDTO menu = menuService.getMenuById(id).orElse(null);
        if (menu != null && menu.getDate() != null && !menu.getDate().isAfter(LocalDate.now())) {
            throw new IllegalArgumentException("Menus can only be deleted for future dates");
        }
        
        if (menuService.deleteMenu(id)) {
            return ResponseEntity.noContent().build();
        } else {
            return ResponseEntity.notFound().build();
        }
    }
}