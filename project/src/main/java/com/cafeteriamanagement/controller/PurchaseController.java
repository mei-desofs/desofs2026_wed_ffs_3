package com.cafeteriamanagement.controller;

import com.cafeteriamanagement.dto.PurchaseDTO;
import com.cafeteriamanagement.service.PurchaseService;
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
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/purchases")
@CrossOrigin(origins = "*")
@Tag(name = "Purchases", description = "Manage cafeteria purchases with balance enforcement")
@SecurityRequirement(name = "bearerAuth")
public class PurchaseController {

    private final PurchaseService purchaseService;

    @Autowired
    public PurchaseController(PurchaseService purchaseService) {
        this.purchaseService = purchaseService;
    }

    @GetMapping
    @Operation(summary = "List purchases", description = "Retrieve purchases for the authenticated user or all purchases for admins")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Purchases retrieved",
            content = @Content(mediaType = "application/json",
                array = @ArraySchema(schema = @Schema(implementation = PurchaseDTO.class))))
    })
    public ResponseEntity<List<PurchaseDTO>> getAllPurchases() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String currentUsername = auth.getName();
        boolean isAdmin = auth.getAuthorities().stream()
            .anyMatch(grantedAuthority -> grantedAuthority.getAuthority().equals("ROLE_ADMIN"));
            
        if (!isAdmin) {
            List<PurchaseDTO> purchases = purchaseService.getPurchasesByClientUsername(currentUsername);
            return ResponseEntity.ok(purchases);
        }
        
        List<PurchaseDTO> purchases = purchaseService.getAllPurchases();
        return ResponseEntity.ok(purchases);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Retrieve purchase", description = "Fetch purchase details by external identifier")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Purchase found",
            content = @Content(mediaType = "application/json",
                schema = @Schema(implementation = PurchaseDTO.class))),
        @ApiResponse(responseCode = "404", description = "Purchase not found", content = @Content)
    })
    public ResponseEntity<PurchaseDTO> getPurchaseById(
        @Parameter(description = "External identifier of the purchase", example = "550e8400-e29b-41d4-a716-446655440030")
        @PathVariable String id) {
        return purchaseService.getPurchaseById(id)
                .map(purchase -> ResponseEntity.ok(purchase))
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/by-client/{clientId}")
    @Operation(summary = "List purchases for client", description = "Retrieve all purchases for a client by external user identifier")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Purchases retrieved",
            content = @Content(mediaType = "application/json",
                array = @ArraySchema(schema = @Schema(implementation = PurchaseDTO.class))))
    })
    public ResponseEntity<List<PurchaseDTO>> getPurchasesByClientId(
        @Parameter(description = "External identifier of the client", example = "550e8400-e29b-41d4-a716-446655440111")
        @PathVariable String clientId) {
        List<PurchaseDTO> purchases = purchaseService.getPurchasesByClientId(clientId);
        return ResponseEntity.ok(purchases);
    }

    @GetMapping("/client/{clientId}")
    @Operation(summary = "List purchases with authorization", description = "Retrieve purchases for a client enforcing ownership for non-admins")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Purchases retrieved",
            content = @Content(mediaType = "application/json",
                array = @ArraySchema(schema = @Schema(implementation = PurchaseDTO.class)))),
        @ApiResponse(responseCode = "403", description = "Forbidden", content = @Content)
    })
    public ResponseEntity<List<PurchaseDTO>> getPurchasesByClient(
        @Parameter(description = "External identifier of the client", example = "550e8400-e29b-41d4-a716-446655440111")
        @PathVariable String clientId) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String currentUsername = auth.getName();
        boolean isAdmin = auth.getAuthorities().stream()
            .anyMatch(grantedAuthority -> grantedAuthority.getAuthority().equals("ROLE_ADMIN"));
            
        if (!isAdmin) {
            String currentUserExternalId = purchaseService.getUserExternalIdByUsername(currentUsername);
            if (!clientId.equals(currentUserExternalId)) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
            }
        }
        
        List<PurchaseDTO> purchases = purchaseService.getPurchasesByClientId(clientId);
        return ResponseEntity.ok(purchases);
    }

    @GetMapping("/date/{date}")
    @Operation(summary = "List purchases by date", description = "Retrieve purchases scheduled for a given date")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Purchases retrieved",
            content = @Content(mediaType = "application/json",
                array = @ArraySchema(schema = @Schema(implementation = PurchaseDTO.class))))
    })
    public ResponseEntity<List<PurchaseDTO>> getPurchasesByDate(
        @Parameter(description = "Date in ISO format (YYYY-MM-DD)", example = "2024-06-01")
        @PathVariable String date) {
        LocalDate purchaseDate = LocalDate.parse(date);
        List<PurchaseDTO> purchases = purchaseService.getPurchasesByDate(purchaseDate);
        return ResponseEntity.ok(purchases);
    }

    @PostMapping
    @Operation(summary = "Create purchase", description = "Create a purchase ensuring balance deduction and future date validation")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "201", description = "Purchase created",
            content = @Content(mediaType = "application/json",
                schema = @Schema(implementation = PurchaseDTO.class))),
        @ApiResponse(responseCode = "400", description = "Validation error", content = @Content)
    })
    public ResponseEntity<PurchaseDTO> createPurchase(
        @io.swagger.v3.oas.annotations.parameters.RequestBody(description = "Purchase payload containing client username, dish name, and future date", required = true,
            content = @Content(schema = @Schema(implementation = PurchaseDTO.class)))
        @Valid @RequestBody PurchaseDTO purchaseDTO) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String currentUsername = auth.getName();
        boolean isAdmin = auth.getAuthorities().stream()
            .anyMatch(grantedAuthority -> grantedAuthority.getAuthority().equals("ROLE_ADMIN"));
            
        if (purchaseDTO.getDate() != null && !purchaseDTO.getDate().isAfter(LocalDate.now())) {
            throw new IllegalArgumentException("Purchases can only be created for future dates");
        }
        
        if (!isAdmin && !currentUsername.equals(purchaseDTO.getClientUsername())) {
            throw new IllegalArgumentException("Clients can only create purchases for themselves");
        }
        
        PurchaseDTO createdPurchase = purchaseService.createPurchase(purchaseDTO);
        return ResponseEntity.status(HttpStatus.CREATED).body(createdPurchase);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update purchase", description = "Update purchase details for future dates with ownership enforcement")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Purchase updated",
            content = @Content(mediaType = "application/json",
                schema = @Schema(implementation = PurchaseDTO.class))),
        @ApiResponse(responseCode = "400", description = "Validation error", content = @Content),
        @ApiResponse(responseCode = "404", description = "Purchase not found", content = @Content)
    })
    public ResponseEntity<PurchaseDTO> updatePurchase(
        @Parameter(description = "External identifier of the purchase", example = "550e8400-e29b-41d4-a716-446655440030")
        @PathVariable String id,
        @io.swagger.v3.oas.annotations.parameters.RequestBody(description = "Updated purchase payload with future date", required = true,
            content = @Content(schema = @Schema(implementation = PurchaseDTO.class)))
        @Valid @RequestBody PurchaseDTO purchaseDTO) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String currentUsername = auth.getName();
        boolean isAdmin = auth.getAuthorities().stream()
            .anyMatch(grantedAuthority -> grantedAuthority.getAuthority().equals("ROLE_ADMIN"));
            
        if (purchaseDTO.getDate() != null && !purchaseDTO.getDate().isAfter(LocalDate.now())) {
            throw new IllegalArgumentException("Purchases can only be edited for future dates");
        }
        
        PurchaseDTO existingPurchase = purchaseService.getPurchaseById(id).orElse(null);
        if (existingPurchase == null) {
            return ResponseEntity.notFound().build();
        }
        
        if (!isAdmin && !currentUsername.equals(existingPurchase.getClientUsername())) {
            throw new IllegalArgumentException("Clients can only edit their own purchases");
        }
        
        return purchaseService.updatePurchase(id, purchaseDTO)
                .map(purchase -> ResponseEntity.ok(purchase))
                .orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete purchase", description = "Delete a purchase scheduled for a future date with ownership enforcement")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "Purchase deleted", content = @Content),
            @ApiResponse(responseCode = "400", description = "Validation error", content = @Content),
            @ApiResponse(responseCode = "404", description = "Purchase not found", content = @Content)
    })
    public ResponseEntity<Void> deletePurchase(
        @Parameter(description = "External identifier of the purchase", example = "550e8400-e29b-41d4-a716-446655440030")
        @PathVariable String id) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String currentUsername = auth.getName();
        boolean isAdmin = auth.getAuthorities().stream()
            .anyMatch(grantedAuthority -> grantedAuthority.getAuthority().equals("ROLE_ADMIN"));
            
        PurchaseDTO existingPurchase = purchaseService.getPurchaseById(id).orElse(null);
        if (existingPurchase == null) {
            return ResponseEntity.notFound().build();
        }
        
        if (existingPurchase.getDate() != null && !existingPurchase.getDate().isAfter(LocalDate.now())) {
            throw new IllegalArgumentException("Purchases can only be deleted for future dates");
        }
        
        if (!isAdmin && !currentUsername.equals(existingPurchase.getClientUsername())) {
            throw new IllegalArgumentException("Clients can only delete their own purchases");
        }
        
        if (purchaseService.deletePurchase(id)) {
            return ResponseEntity.noContent().build();
        } else {
            return ResponseEntity.notFound().build();
        }
    }
}