package com.example.fruitshop.dal;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import com.example.fruitshop.contracts.OrderDataSource;
import com.example.fruitshop.core.BusinessClock;
import com.example.fruitshop.core.BusinessDates;
import com.example.fruitshop.core.DomainException;
import com.example.fruitshop.core.ErrorCode;
import com.example.fruitshop.core.Money;
import com.example.fruitshop.core.Quantities;
import com.example.fruitshop.dto.CartLine;
import com.example.fruitshop.dto.OrderDetails;
import com.example.fruitshop.models.Customer;
import com.example.fruitshop.models.Fruit;
import com.example.fruitshop.models.Order;
import com.example.fruitshop.models.OrderItem;
import com.example.fruitshop.models.OrderStatus;
import com.example.fruitshop.models.Role;
import com.example.fruitshop.models.SaleUnit;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Owner: person 3. Synchronous; run on FruitShopApp.databaseExecutor. */
public final class OrderRepository implements OrderDataSource {
    private static final String ORDER_COLUMNS = "SELECT id,customerId,orderDate,totalAmount,status FROM \"Order\" ";
    private static final String OWNED_BY_ACCOUNT = "customerId=(SELECT id FROM Customer WHERE accountId=?)";
    private final DBHelper helper;
    private final AuthRepository auth;
    private final BusinessClock clock;

    public OrderRepository(DBHelper helper, AuthRepository auth, BusinessClock clock) {
        this.helper = helper; this.auth = auth; this.clock = clock;
    }

    /** One transaction: re-check price/expiry/stock, insert Order + OrderItem, subtract stock. */
    @Override public long place(long actorAccountId, List<CartLine> lines) {
        Customer customer = auth.requireCustomer(actorAccountId);
        Map<Long, CartLine> merged = merge(lines);
        SQLiteDatabase db = helper.getWritableDatabase();
        db.beginTransaction();
        try {
            long total = 0;
            List<ContentValues> items = new ArrayList<>();
            for (CartLine line : merged.values()) {
                Fruit fruit = fruit(db, line.fruitId);
                if (fruit == null) throw new DomainException(ErrorCode.NOT_FOUND, "Có món trong giỏ không còn bán.");
                if (BusinessDates.isExpired(fruit.expiryDate, clock)) {
                    throw new DomainException(ErrorCode.EXPIRED, fruit.name + " đã hết hạn, không thể đặt.");
                }
                if (fruit.price != line.quotedUnitPrice) {
                    throw new DomainException(ErrorCode.PRICE_CHANGED, "Giá " + fruit.name + " đã thay đổi. Vui lòng xem lại giỏ hàng.");
                }
                if (line.quantity > fruit.quantity) {
                    throw new DomainException(ErrorCode.OUT_OF_STOCK,
                            fruit.name + " chỉ còn " + Quantities.format(fruit.quantity, fruit.unit) + ".");
                }
                long lineTotal = Money.lineTotal(fruit.price, line.quantity, fruit.unit);
                total = Money.add(total, lineTotal);
                db.execSQL("UPDATE Fruit SET quantity=quantity-? WHERE id=?", new Object[]{line.quantity, fruit.id});
                ContentValues item = new ContentValues();
                item.put("fruitId", fruit.id); item.put("fruitName", fruit.name); item.put("unit", fruit.unit.name());
                item.put("unitPrice", fruit.price); item.put("quantity", line.quantity); item.put("lineTotal", lineTotal);
                items.add(item);
            }
            ContentValues order = new ContentValues();
            order.put("customerId", customer.id); order.put("orderDate", clock.nowMillis());
            order.put("totalAmount", total); order.put("status", OrderStatus.PENDING.name());
            long orderId = db.insertOrThrow("\"Order\"", null, order);
            for (ContentValues item : items) {
                item.put("orderId", orderId);
                db.insertOrThrow("OrderItem", null, item);
            }
            db.setTransactionSuccessful();
            return orderId;
        } finally { db.endTransaction(); }
    }

    /** Admin: every order. Customer: only orders of the signed-in customer. */
    @Override public List<Order> list(long actorAccountId) {
        boolean admin = auth.accountById(actorAccountId).role == Role.ADMIN;
        return orders(ORDER_COLUMNS + (admin ? "" : "WHERE " + OWNED_BY_ACCOUNT) + " ORDER BY id DESC",
                admin ? null : args(actorAccountId));
    }

    /** Someone else's order is reported as NOT_FOUND so its existence is not revealed. */
    @Override public OrderDetails detail(long actorAccountId, long orderId) {
        boolean admin = auth.accountById(actorAccountId).role == Role.ADMIN;
        List<Order> found = orders(ORDER_COLUMNS + "WHERE id=?" + (admin ? "" : " AND " + OWNED_BY_ACCOUNT),
                admin ? args(orderId) : args(orderId, actorAccountId));
        if (found.isEmpty()) throw notFound();
        List<OrderItem> items = new ArrayList<>();
        try (Cursor c = helper.getReadableDatabase().rawQuery("SELECT id,orderId,fruitId,fruitName,unit,unitPrice,quantity,lineTotal "
                + "FROM OrderItem WHERE orderId=? ORDER BY id", args(orderId))) {
            while (c.moveToNext()) {
                items.add(new OrderItem(c.getLong(0), c.getLong(1), c.isNull(2) ? null : c.getLong(2), c.getString(3),
                        SaleUnit.valueOf(c.getString(4)), c.getLong(5), c.getLong(6), c.getLong(7)));
            }
        }
        Order order = found.get(0);
        Customer customer;
        // Read contact information only after checking access to this order.
        try (Cursor c = helper.getReadableDatabase().rawQuery(
                "SELECT id,fullname,phone,email,accountId FROM Customer WHERE id=?", args(order.customerId))) {
            if (!c.moveToFirst()) throw notFound();
            customer = new Customer(c.getLong(0), c.getString(1), c.getString(2), c.getString(3), c.getLong(4));
        }
        return new OrderDetails(order, customer, items);
    }

    /** Admin only, one step at a time. Stock was already subtracted when the order was placed. */
    @Override public void advanceStatus(long actorAccountId, long orderId, OrderStatus nextStatus) {
        auth.requireAdmin(actorAccountId);
        SQLiteDatabase db = helper.getWritableDatabase();
        db.beginTransaction();
        try {
            OrderStatus current;
            try (Cursor c = db.rawQuery("SELECT status FROM \"Order\" WHERE id=?", args(orderId))) {
                if (!c.moveToFirst()) throw notFound();
                current = OrderStatus.valueOf(c.getString(0));
            }
            if (nextStatus == null || nextStatus != next(current)) {
                throw new DomainException(ErrorCode.INVALID_STATUS, "Đơn chỉ chuyển theo thứ tự Chờ xác nhận → Đã xác nhận → Hoàn thành.");
            }
            ContentValues values = new ContentValues();
            values.put("status", nextStatus.name());
            db.update("\"Order\"", values, "id=?", args(orderId));
            db.setTransactionSuccessful();
        } finally { db.endTransaction(); }
    }

    /** PENDING → CONFIRMED → COMPLETED; null when the order is already completed. */
    private static OrderStatus next(OrderStatus status) {
        if (status == OrderStatus.PENDING) return OrderStatus.CONFIRMED;
        if (status == OrderStatus.CONFIRMED) return OrderStatus.COMPLETED;
        return null;
    }

    /** Cart screen helper: current name/price/total of each cart line. Changes nothing; missing fruits are skipped. */
    public List<OrderItem> preview(long actorAccountId, List<CartLine> lines) {
        auth.requireCustomer(actorAccountId);
        SQLiteDatabase db = helper.getReadableDatabase();
        List<OrderItem> result = new ArrayList<>();
        for (CartLine line : lines) {
            Fruit fruit = fruit(db, line.fruitId);
            if (fruit == null) continue;
            String name = fruit.name;
            if (BusinessDates.isExpired(fruit.expiryDate, clock)) name += " — ĐÃ HẾT HẠN";
            else if (line.quantity > fruit.quantity) name += " — KHÔNG ĐỦ HÀNG";
            result.add(new OrderItem(0, 0, fruit.id, name, fruit.unit, fruit.price, line.quantity,
                    Money.lineTotal(fruit.price, line.quantity, fruit.unit)));
        }
        return result;
    }

    private static Map<Long, CartLine> merge(List<CartLine> lines) {
        if (lines == null || lines.isEmpty()) throw new DomainException(ErrorCode.INVALID_INPUT, "Giỏ hàng đang trống.");
        Map<Long, CartLine> merged = new LinkedHashMap<>();
        for (CartLine line : lines) {
            if (line == null || line.quantity <= 0) throw new DomainException(ErrorCode.INVALID_INPUT, "Số lượng phải lớn hơn 0.");
            CartLine old = merged.get(line.fruitId);
            long quantity = line.quantity;
            if (old != null) {
                try { quantity = Math.addExact(old.quantity, line.quantity); }
                catch (ArithmeticException e) { throw new DomainException(ErrorCode.INVALID_INPUT, "Số lượng quá lớn."); }
            }
            merged.put(line.fruitId, new CartLine(line.fruitId, quantity, line.quotedUnitPrice));
        }
        return merged;
    }

    private static Fruit fruit(SQLiteDatabase db, long id) {
        try (Cursor c = db.rawQuery("SELECT id,name,category,price,quantity,expiryDate,description,unit FROM Fruit WHERE id=?", args(id))) {
            if (!c.moveToFirst()) return null;
            return new Fruit(c.getLong(0), c.getString(1), c.getString(2), c.getLong(3), c.getLong(4),
                    c.getString(5), c.getString(6), SaleUnit.valueOf(c.getString(7)));
        }
    }

    private List<Order> orders(String sql, String[] args) {
        List<Order> result = new ArrayList<>();
        try (Cursor c = helper.getReadableDatabase().rawQuery(sql, args)) {
            while (c.moveToNext()) {
                result.add(new Order(c.getLong(0), c.getLong(1), c.getLong(2), c.getLong(3), OrderStatus.valueOf(c.getString(4))));
            }
        }
        return result;
    }

    private static String[] args(long... values) {
        String[] result = new String[values.length];
        for (int i = 0; i < values.length; i++) result[i] = Long.toString(values[i]);
        return result;
    }

    private static DomainException notFound() { return new DomainException(ErrorCode.NOT_FOUND, "Không tìm thấy đơn hàng."); }
}
