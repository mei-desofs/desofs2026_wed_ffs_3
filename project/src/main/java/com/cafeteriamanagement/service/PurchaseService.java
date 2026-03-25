package com.cafeteriamanagement.service;

import com.cafeteriamanagement.dto.PurchaseDTO;
import com.cafeteriamanagement.model.entity.Dish;
import com.cafeteriamanagement.model.entity.Menu;
import com.cafeteriamanagement.model.entity.Purchase;
import com.cafeteriamanagement.model.entity.User;
import com.cafeteriamanagement.repository.PurchaseRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@Transactional
public class PurchaseService {

    private final PurchaseRepository purchaseRepository;
    private final UserService userService;
    private final DishService dishService;
    private final MenuService menuService;

    @Autowired
    public PurchaseService(PurchaseRepository purchaseRepository, UserService userService, DishService dishService, MenuService menuService) {
        this.purchaseRepository = purchaseRepository;
        this.userService = userService;
        this.dishService = dishService;
        this.menuService = menuService;
    }

    public List<PurchaseDTO> getAllPurchases() {
        return purchaseRepository.findAll().stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    public Optional<PurchaseDTO> getPurchaseById(String externalId) {
        return purchaseRepository.findByExternalId(externalId)
                .map(this::convertToDTO);
    }

    public List<PurchaseDTO> getPurchasesByClientId(String clientId) {
        User client = userService.findByExternalId(clientId);
        return purchaseRepository.findByClient(client).stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    public List<PurchaseDTO> getPurchasesByClientUsername(String username) {
        User client = userService.findByUsername(username);
        return purchaseRepository.findByClient(client).stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    public List<PurchaseDTO> getPurchasesByDate(LocalDate date) {
        return purchaseRepository.findByDate(date).stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    public PurchaseDTO createPurchase(PurchaseDTO purchaseDTO) {
    User client = userService.findByUsername(purchaseDTO.getClientUsername());
    Dish dish = dishService.findByName(purchaseDTO.getDishName());
    validateDishAvailabilityInMenu(dish, purchaseDTO.getDate());
    client.deductBalance(dish.getPrice());
    Purchase purchase = new Purchase(client, dish, purchaseDTO.getDate());
    Purchase savedPurchase = purchaseRepository.save(purchase);
    return convertToDTO(savedPurchase);
    }

    public Optional<PurchaseDTO> updatePurchase(String externalId, PurchaseDTO purchaseDTO) {
        return purchaseRepository.findByExternalId(externalId)
                .map(purchase -> {
                    purchase.getClient().addBalance(purchase.getDish().getPrice());
                    User newClient = userService.findByUsername(purchaseDTO.getClientUsername());
                    Dish newDish = dishService.findByName(purchaseDTO.getDishName());
                    validateDishAvailabilityInMenu(newDish, purchaseDTO.getDate());
                    newClient.deductBalance(newDish.getPrice());
                    purchase.updateDetails(newClient, newDish, purchaseDTO.getDate());
                    Purchase savedPurchase = purchaseRepository.save(purchase);
                    return convertToDTO(savedPurchase);
                });
    }

    public boolean deletePurchase(String externalId) {
        return purchaseRepository.findByExternalId(externalId)
                .map(purchase -> {
                    purchase.getClient().addBalance(purchase.getDish().getPrice());
                    purchaseRepository.delete(purchase);
                    return true;
                })
                .orElse(false);
    }

    public String getUserExternalIdByUsername(String username) {
        User user = userService.findByUsername(username);
        return user.getExternalId();
    }

    private PurchaseDTO convertToDTO(Purchase purchase) {
        PurchaseDTO dto = new PurchaseDTO();
        dto.setId(purchase.getExternalId());
        dto.setClientUsername(purchase.getClient().getUsername());
        dto.setDishName(purchase.getDish().getName().getValue());
        dto.setDate(purchase.getDate());
        return dto;
    }
    
    private void validateDishAvailabilityInMenu(Dish dish, LocalDate date) {
        try {
            Menu menu = menuService.findByDate(date);
            boolean dishAvailable = false;
            if (menu.getMeatDish() != null && menu.getMeatDish().getId().equals(dish.getId())) {
                dishAvailable = true;
            } else if (menu.getFishDish() != null && menu.getFishDish().getId().equals(dish.getId())) {
                dishAvailable = true;
            } else if (menu.getVegetarianDish() != null && menu.getVegetarianDish().getId().equals(dish.getId())) {
                dishAvailable = true;
            }
            if (!dishAvailable) {
                throw new IllegalArgumentException("Dish '" + dish.getName().getValue() + "' is not available in the menu for date " + date);
            }
        } catch (IllegalArgumentException e) {
            if (e.getMessage().contains("Menu not found")) {
                throw new IllegalArgumentException("No menu available for date " + date);
            }
            throw e;
        }
    }
}