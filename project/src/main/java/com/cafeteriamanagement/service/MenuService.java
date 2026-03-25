package com.cafeteriamanagement.service;

import com.cafeteriamanagement.dto.MenuDTO;
import com.cafeteriamanagement.model.entity.Dish;
import com.cafeteriamanagement.model.entity.Menu;
import com.cafeteriamanagement.repository.MenuRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@Transactional
public class MenuService {

    private final MenuRepository menuRepository;
    private final DishService dishService;

    @Autowired
    public MenuService(MenuRepository menuRepository, DishService dishService) {
        this.menuRepository = menuRepository;
        this.dishService = dishService;
    }

    public List<MenuDTO> getAllMenus() {
        return menuRepository.findAll().stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    public Optional<MenuDTO> getMenuById(String externalId) {
        return menuRepository.findByExternalId(externalId)
                .map(this::convertToDTO);
    }

    public Optional<MenuDTO> getMenuByDate(LocalDate date) {
        return menuRepository.findByDate(date)
                .map(this::convertToDTO);
    }
    
    public Menu findByDate(LocalDate date) {
        return menuRepository.findByDate(date)
                .orElseThrow(() -> new IllegalArgumentException("Menu not found for date: " + date));
    }

    public MenuDTO createMenu(MenuDTO menuDTO) {
        if (menuRepository.existsByDate(menuDTO.getDate())) {
            throw new IllegalArgumentException("A menu already exists for date " + menuDTO.getDate());
        }
        Menu menu = convertToEntity(menuDTO);
        Menu savedMenu = menuRepository.save(menu);
        return convertToDTO(savedMenu);
    }

    public Optional<MenuDTO> updateMenu(String externalId, MenuDTO menuDTO) {
        return menuRepository.findByExternalId(externalId)
                .map(menu -> {
                    if (!menu.getDate().equals(menuDTO.getDate()) && 
                        menuRepository.existsByDate(menuDTO.getDate())) {
                        throw new IllegalArgumentException("Menu already exists for date: " + menuDTO.getDate());
                    }
                    List<String> missingDishes = new ArrayList<>();
                    Dish meatDish = null;
                    Dish fishDish = null;
                    Dish vegetarianDish = null;
                    if (menuDTO.getMeatDishName() != null) {
                        try {
                            meatDish = dishService.findByName(menuDTO.getMeatDishName());
                        } catch (IllegalArgumentException e) {
                            missingDishes.add(menuDTO.getMeatDishName());
                        }
                    }
                    if (menuDTO.getFishDishName() != null) {
                        try {
                            fishDish = dishService.findByName(menuDTO.getFishDishName());
                        } catch (IllegalArgumentException e) {
                            missingDishes.add(menuDTO.getFishDishName());
                        }
                    }
                    if (menuDTO.getVegetarianDishName() != null) {
                        try {
                            vegetarianDish = dishService.findByName(menuDTO.getVegetarianDishName());
                        } catch (IllegalArgumentException e) {
                            missingDishes.add(menuDTO.getVegetarianDishName());
                        }
                    }
                    if (!missingDishes.isEmpty()) {
                        String errorMessage = missingDishes.size() == 1 
                            ? "Dish not found: " + missingDishes.get(0)
                            : "Dishes not found: " + String.join(", ", missingDishes);
                        throw new IllegalArgumentException(errorMessage);
                    }
                    menu.updateDetails(menuDTO.getDate(), meatDish, fishDish, vegetarianDish);
                    Menu savedMenu = menuRepository.save(menu);
                    return convertToDTO(savedMenu);
                });
    }

    public boolean deleteMenu(String externalId) {
        return menuRepository.findByExternalId(externalId)
                .map(menu -> {
                    menuRepository.delete(menu);
                    return true;
                })
                .orElse(false);
    }

    private MenuDTO convertToDTO(Menu menu) {
        MenuDTO dto = new MenuDTO();
        dto.setId(menu.getExternalId());
        dto.setDate(menu.getDate());
        dto.setMeatDishName(menu.getMeatDish() != null ? menu.getMeatDish().getName().getValue() : null);
        dto.setFishDishName(menu.getFishDish() != null ? menu.getFishDish().getName().getValue() : null);
        dto.setVegetarianDishName(menu.getVegetarianDish() != null ? menu.getVegetarianDish().getName().getValue() : null);
        return dto;
    }

    private Menu convertToEntity(MenuDTO dto) {
        List<String> missingDishes = new ArrayList<>();
        Dish meatDish = null;
        Dish fishDish = null;
        Dish vegetarianDish = null;
        if (dto.getMeatDishName() != null) {
            try {
                meatDish = dishService.findByName(dto.getMeatDishName());
            } catch (IllegalArgumentException e) {
                missingDishes.add(dto.getMeatDishName());
            }
        }
        if (dto.getFishDishName() != null) {
            try {
                fishDish = dishService.findByName(dto.getFishDishName());
            } catch (IllegalArgumentException e) {
                missingDishes.add(dto.getFishDishName());
            }
        }
        if (dto.getVegetarianDishName() != null) {
            try {
                vegetarianDish = dishService.findByName(dto.getVegetarianDishName());
            } catch (IllegalArgumentException e) {
                missingDishes.add(dto.getVegetarianDishName());
            }
        }
        if (!missingDishes.isEmpty()) {
            String errorMessage = missingDishes.size() == 1 
                ? "Dish not found: " + missingDishes.get(0)
                : "Dishes not found: " + String.join(", ", missingDishes);
            throw new IllegalArgumentException(errorMessage);
        }
        return new Menu(dto.getDate(), meatDish, fishDish, vegetarianDish);
    }
    
    public boolean isFutureDate(LocalDate date) {
        return date != null && date.isAfter(LocalDate.now());
    }
}