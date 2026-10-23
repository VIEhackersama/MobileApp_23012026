package com.example.fruitshop.models;

public enum SaleUnit {
    KG("kg"), PIECE("quả"), PACK("phần");
    public final String label;
    SaleUnit(String label) { this.label = label; }
}
