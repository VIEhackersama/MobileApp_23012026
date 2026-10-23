package com.example.fruitshop.models;

/** price: VND per sale unit; quantity: grams for KG, integer count otherwise. */
public final class Fruit {
    public final long id, price, quantity;
    public final String name, category, expiryDate, description;
    public final SaleUnit unit;
    public Fruit(long id, String name, String category, long price, long quantity,
                 String expiryDate, String description, SaleUnit unit) {
        this.id = id; this.name = name; this.category = category; this.price = price; this.quantity = quantity;
        this.expiryDate = expiryDate; this.description = description; this.unit = unit;
    }
}
