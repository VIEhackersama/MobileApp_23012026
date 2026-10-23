package com.example.fruitshop.models;

public final class Order {
    public final long id, customerId, orderDate, totalAmount;
    public final OrderStatus status;
    /** orderDate is Unix epoch milliseconds, totalAmount is integer VND. */
    public Order(long id, long customerId, long orderDate, long totalAmount, OrderStatus status) {
        this.id = id; this.customerId = customerId; this.orderDate = orderDate;
        this.totalAmount = totalAmount; this.status = status;
    }
}
