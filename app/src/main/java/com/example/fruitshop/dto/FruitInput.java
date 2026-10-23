package com.example.fruitshop.dto;

import com.example.fruitshop.models.SaleUnit;

public final class FruitInput {
    public final String name, category, expiryDate, description;
    public final long price, quantity;
    public final SaleUnit unit;
    public FruitInput(String name, String category, long price, long quantity, String expiryDate,
                      String description, SaleUnit unit) {
        this.name = name; this.category = category; this.price = price; this.quantity = quantity;
        this.expiryDate = expiryDate; this.description = description; this.unit = unit;
    }
}
