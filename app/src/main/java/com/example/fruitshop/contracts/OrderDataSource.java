package com.example.fruitshop.contracts;

import com.example.fruitshop.dto.CartLine;
import com.example.fruitshop.dto.OrderDetails;
import com.example.fruitshop.models.Order;
import com.example.fruitshop.models.OrderStatus;
import java.util.List;

/** Owner: person 3. No implementation is supplied by the foundation module. */
public interface OrderDataSource {
    long place(long actorAccountId, List<CartLine> lines);
    List<Order> list(long actorAccountId);
    OrderDetails detail(long actorAccountId, long orderId);
    void advanceStatus(long actorAccountId, long orderId, OrderStatus nextStatus);
}
