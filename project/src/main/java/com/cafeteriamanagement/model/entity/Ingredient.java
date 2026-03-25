package com.cafeteriamanagement.model.entity;

import com.cafeteriamanagement.model.enums.Allergen;
import com.cafeteriamanagement.model.enums.IngredientType;
import com.cafeteriamanagement.model.valueobject.Name;
import jakarta.persistence.*;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

import java.util.Objects;
import java.util.UUID;

@Entity
@Table(name = "ingredients")
public class Ingredient {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "external_id", unique = true, nullable = false)
    private String externalId;

    @Embedded
    @Valid
    @NotNull(message = "Name is required")
    @AttributeOverrides({
        @AttributeOverride(name = "value", column = @Column(name = "name", unique = true))
    })
    private Name name;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @NotNull(message = "Type is required")
    private IngredientType type;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @NotNull(message = "Allergen is required")
    private Allergen allergen;

    protected Ingredient() {}

    public Ingredient(Name name, IngredientType type, Allergen allergen) {
        this.externalId = UUID.randomUUID().toString();
        this.name = name;
        this.type = type;
        this.allergen = allergen;
    }

    public Long getId() {
        return id;
    }

    public String getExternalId() {
        return externalId;
    }

    public Name getName() {
        return name;
    }

    public IngredientType getType() {
        return type;
    }

    public Allergen getAllergen() {
        return allergen;
    }

    public void updateDetails(Name name, IngredientType type, Allergen allergen) {
        this.name = name;
        this.type = type;
        this.allergen = allergen;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Ingredient that = (Ingredient) o;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return "Ingredient{" +
                "externalId='" + externalId + '\'' +
                ", name=" + name +
                ", type=" + type +
                ", allergen=" + allergen +
                '}';
    }
}