package com.example.fruitshop.cart;

import com.example.fruitshop.contracts.CartStore;
import com.example.fruitshop.core.DomainException;
import com.example.fruitshop.core.ErrorCode;
import com.example.fruitshop.dto.CartLine;
import com.example.fruitshop.session.SessionManager;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Owner: person 3. In memory only: kept across rotation, cleared on logout/account switch or process death. */
public final class MemoryCartStore implements CartStore, SessionManager.Listener {
    private final SessionManager session;
    private final Map<Long, Map<Long, CartLine>> carts = new HashMap<>(); // accountId -> (fruitId -> line)

    public MemoryCartStore(SessionManager session) {
        this.session = session;
        session.addListener(this);
    }

    @Override public synchronized List<CartLine> lines(long actorAccountId) {
        Map<Long, CartLine> cart = carts.get(requireCurrent(actorAccountId));
        if (cart == null) return Collections.emptyList();
        return Collections.unmodifiableList(new ArrayList<>(cart.values()));
    }

    /** Same fruit twice: quantities are added and the newest quoted price is kept. */
    @Override public synchronized void add(long actorAccountId, CartLine line) {
        if (line == null || line.quantity <= 0 || line.quotedUnitPrice <= 0) throw invalid();
        Map<Long, CartLine> cart = carts.get(requireCurrent(actorAccountId));
        if (cart == null) carts.put(actorAccountId, cart = new LinkedHashMap<>());
        CartLine old = cart.get(line.fruitId);
        long quantity = line.quantity;
        if (old != null) {
            try { quantity = Math.addExact(old.quantity, line.quantity); }
            catch (ArithmeticException e) { throw invalid(); }
        }
        cart.put(line.fruitId, new CartLine(line.fruitId, quantity, line.quotedUnitPrice));
    }

    @Override public synchronized void setQuantity(long actorAccountId, long fruitId, long baseQuantity) {
        if (baseQuantity <= 0) throw invalid();
        Map<Long, CartLine> cart = carts.get(requireCurrent(actorAccountId));
        CartLine old = cart == null ? null : cart.get(fruitId);
        if (old == null) throw new DomainException(ErrorCode.NOT_FOUND, "Món này không có trong giỏ.");
        cart.put(fruitId, new CartLine(fruitId, baseQuantity, old.quotedUnitPrice));
    }

    @Override public synchronized void remove(long actorAccountId, long fruitId) {
        Map<Long, CartLine> cart = carts.get(requireCurrent(actorAccountId));
        if (cart != null) cart.remove(fruitId);
    }

    /** No session check: the listener calls this with the old account id after logout. */
    @Override public synchronized void clear(long actorAccountId) { carts.remove(actorAccountId); }

    @Override public void onSessionChanged(long previousAccountId, long currentAccountId) {
        if (previousAccountId != currentAccountId) clear(previousAccountId);
    }

    private long requireCurrent(long actorAccountId) {
        if (actorAccountId < 0 || actorAccountId != session.accountId()) {
            throw new DomainException(ErrorCode.FORBIDDEN, "Giỏ hàng chỉ dùng cho tài khoản đang đăng nhập.");
        }
        return actorAccountId;
    }

    private static DomainException invalid() {
        return new DomainException(ErrorCode.INVALID_INPUT, "Số lượng phải lớn hơn 0.");
    }
}