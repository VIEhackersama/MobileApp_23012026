package com.example.fruitshop.dto;

/** quote is the price last reviewed by the customer, not an authoritative checkout price. */
public final class CartLine {
    public final long fruitId, quantity, quotedUnitPrice;
    public CartLine(long fruitId, long quantity, long quotedUnitPrice) {
        this.fruitId = fruitId; this.quantity = quantity; this.quotedUnitPrice = quotedUnitPrice;
    }
}
