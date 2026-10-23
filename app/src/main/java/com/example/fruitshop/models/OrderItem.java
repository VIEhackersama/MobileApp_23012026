package com.example.fruitshop.models;

/** Historical snapshots; nullable fruitId allows deleting a catalog item without losing history. */
public final class OrderItem {
    public final long id, orderId, unitPrice, quantity, lineTotal;
    public final Long fruitId;
    public final String fruitName;
    public final SaleUnit unit;
    public OrderItem(long id, long orderId, Long fruitId, String fruitName, SaleUnit unit,
                     long unitPrice, long quantity, long lineTotal) {
        this.id = id; this.orderId = orderId; this.fruitId = fruitId; this.fruitName = fruitName;
        this.unit = unit; this.unitPrice = unitPrice; this.quantity = quantity; this.lineTotal = lineTotal;
    }
}
