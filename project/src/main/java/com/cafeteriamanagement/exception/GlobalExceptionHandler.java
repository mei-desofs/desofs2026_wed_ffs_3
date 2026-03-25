package com.cafeteriamanagement.exception;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.HashMap;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, String>> handleValidationExceptions(MethodArgumentNotValidException ex) {
        Map<String, String> errors = new HashMap<>();
        ex.getBindingResult().getAllErrors().forEach((error) -> {
            String fieldName = ((FieldError) error).getField();
            String errorMessage = error.getDefaultMessage();
            errors.put(fieldName, errorMessage);
        });
        return ResponseEntity.badRequest().body(errors);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, String>> handleIllegalArgumentException(IllegalArgumentException ex) {
        Map<String, String> error = new HashMap<>();
        error.put("error", ex.getMessage());
        return ResponseEntity.badRequest().body(error);
    }

    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<Map<String, String>> handleIllegalStateException(IllegalStateException ex) {
        Map<String, String> error = new HashMap<>();
        error.put("error", ex.getMessage());
        return ResponseEntity.badRequest().body(error);
    }

    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<Map<String, String>> handleBadCredentialsException(BadCredentialsException ex) {
        Map<String, String> error = new HashMap<>();
        error.put("error", ex.getMessage());
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(error);
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<Map<String, String>> handleDataIntegrityViolationException(DataIntegrityViolationException ex) {
        Map<String, String> error = new HashMap<>();
        String message = ex.getMessage();
        
        if (message != null && message.contains("CONSTRAINT_INDEX")) {
            if (message.contains("INGREDIENTS(NAME") && message.contains("VALUES")) {
                String name = extractValueFromConstraintMessage(message);
                error.put("error", "Ingredient with name '" + name + "' already exists");
            } else if (message.contains("DISHES(NAME") && message.contains("VALUES")) {
                String name = extractValueFromConstraintMessage(message);
                error.put("error", "Dish with name '" + name + "' already exists");
            } else if (message.contains("USERS(USERNAME") && message.contains("VALUES")) {
                String username = extractValueFromConstraintMessage(message);
                error.put("error", "User with username '" + username + "' already exists");
            } else if (message.contains("MENUS(DATE") && message.contains("VALUES")) {
                String date = extractValueFromConstraintMessage(message);
                error.put("error", "A menu already exists for date " + date);
            } else {
                error.put("error", "A record with this name already exists");
            }
        } else {
            error.put("error", "A record with this information already exists");
        }
        
        return ResponseEntity.badRequest().body(error);
    }

    private String extractValueFromConstraintMessage(String message) {
        try {
            int startIndex = message.indexOf("VALUES ( /* ");
            if (startIndex != -1) {
                int quoteStart = message.indexOf("'", startIndex);
                if (quoteStart != -1) {
                    int quoteEnd = message.indexOf("'", quoteStart + 1);
                    if (quoteEnd != -1) {
                        return message.substring(quoteStart + 1, quoteEnd);
                    }
                }
            }
        } catch (Exception e) {
        }
        return "this value";
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, String>> handleGenericException(Exception ex) {
        Map<String, String> error = new HashMap<>();
        error.put("error", "An unexpected error occurred: " + ex.getMessage());
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
    }
}