package com.example.fruitshop.contracts;

import com.example.fruitshop.dto.CartLine;
import java.util.List;

/** Owner: person 3. In-memory, per-account; integrate with SessionManager.Listener. */
public interface CartStore {
    List<CartLine> lines(long actorAccountId);
    void add(long actorAccountId, CartLine line);
    void setQuantity(long actorAccountId, long fruitId, long baseQuantity);
    void remove(long actorAccountId, long fruitId);
    void clear(long actorAccountId);
}
