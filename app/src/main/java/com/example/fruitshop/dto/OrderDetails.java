package com.example.fruitshop.dto;

import com.example.fruitshop.models.Order;
import com.example.fruitshop.models.OrderItem;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class OrderDetails {
    public final Order order;
    public final List<OrderItem> items;
    public OrderDetails(Order order, List<OrderItem> items) {
        this.order = order;
        this.items = Collections.unmodifiableList(new ArrayList<>(items));
    }
}
