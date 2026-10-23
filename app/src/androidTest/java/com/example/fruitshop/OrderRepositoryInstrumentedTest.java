package com.example.fruitshop;

import android.content.Context;
import android.database.Cursor;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;
import com.example.fruitshop.core.BusinessClock;
import com.example.fruitshop.core.DomainException;
import com.example.fruitshop.core.ErrorCode;
import com.example.fruitshop.dal.AuthRepository;
import com.example.fruitshop.dal.DBHelper;
import com.example.fruitshop.dal.OrderRepository;
import com.example.fruitshop.dto.CartLine;
import com.example.fruitshop.dto.OrderDetails;
import com.example.fruitshop.models.OrderItem;
import com.example.fruitshop.models.OrderStatus;
import java.util.Arrays;
import org.junit.*;
import org.junit.runner.RunWith;
import static org.junit.Assert.*;

/** Owner: person 3. Uses the seed data: Cam sành 40.000/kg, Táo đỏ 15.000/quả, Trái cây cắt sẵn 25.000/phần. */
@RunWith(AndroidJUnit4.class)
public class OrderRepositoryInstrumentedTest {
    private static final String NAME = "orders_test.db";
    private Context context;
    private DBHelper helper;
    private OrderRepository orders;
    private long admin, customer1, customer2;

    @Before public void setup() {
        context = InstrumentationRegistry.getInstrumentation().getTargetContext();
        context.deleteDatabase(NAME);
        BusinessClock clock = BusinessClock.fixed(1792717200000L, "Asia/Bangkok");
        helper = new DBHelper(context, NAME, clock);
        AuthRepository auth = new AuthRepository(helper);
        orders = new OrderRepository(helper, auth, clock);
        admin = auth.login("admin01", "admin123").id;
        customer1 = auth.login("customer01", "customer123").id;
        customer2 = auth.login("customer02", "customer123").id;
    }

    @After public void cleanup() { helper.close(); context.deleteDatabase(NAME); }

    @Test public void mixedUnitsTotal75000AndSubtractStock() {
        long id = orders.place(customer1, Arrays.asList(
                new CartLine(fruit("Cam sành"), 500, 40000),
                new CartLine(fruit("Táo đỏ"), 2, 15000),
                new CartLine(fruit("Trái cây cắt sẵn"), 1, 25000)));
        OrderDetails details = orders.detail(customer1, id);
        assertEquals(75000, details.order.totalAmount);
        assertEquals(OrderStatus.PENDING, details.order.status);
        assertEquals(3, details.items.size());
        assertNotNull(details.customer);
        assertEquals(details.order.customerId, details.customer.id);
        assertEquals(customer1, details.customer.accountId);
        assertEquals("Nguyễn Minh An", details.customer.fullname);
        assertEquals("0901234567", details.customer.phone);
        assertEquals("minhan@example.com", details.customer.email);
        assertEquals(11500, stock("Cam sành"));
        assertEquals(38, stock("Táo đỏ"));
        assertEquals(14, stock("Trái cây cắt sẵn"));
    }

    @Test public void oneBadLineRollsBackWholeOrder() {
        expect(ErrorCode.OUT_OF_STOCK, () -> orders.place(customer1, Arrays.asList(
                new CartLine(fruit("Táo đỏ"), 2, 15000),
                new CartLine(fruit("Dưa lưới"), 1, 45000))));
        assertEquals(0, count("Order"));
        assertEquals(0, count("OrderItem"));
        assertEquals(40, stock("Táo đỏ"));
    }

    @Test public void expiredBlockedButExpiringTodayAllowed() {
        expect(ErrorCode.EXPIRED, () -> orders.place(customer1, Arrays.asList(new CartLine(fruit("Dâu tây"), 500, 120000))));
        orders.place(customer1, Arrays.asList(new CartLine(fruit("Chuối chín"), 1000, 30000)));
        assertEquals(1, count("Order"));
    }

    @Test public void changedPriceIsRejectedAndAdminCannotOrder() {
        expect(ErrorCode.PRICE_CHANGED, () -> orders.place(customer1, Arrays.asList(new CartLine(fruit("Cam sành"), 500, 39000))));
        expect(ErrorCode.FORBIDDEN, () -> orders.place(admin, Arrays.asList(new CartLine(fruit("Cam sành"), 500, 40000))));
        expect(ErrorCode.INVALID_INPUT, () -> orders.place(customer1, Arrays.asList()));
        assertEquals(0, count("Order"));
    }

    @Test public void historyKeepsSnapshotAfterFruitChangesOrIsDeleted() {
        long apple = fruit("Táo đỏ");
        long id = orders.place(customer1, Arrays.asList(new CartLine(apple, 2, 15000)));
        helper.getWritableDatabase().execSQL("UPDATE Fruit SET name='Táo mới', price=99000 WHERE id=?", new Object[]{apple});
        helper.getWritableDatabase().execSQL("DELETE FROM Fruit WHERE id=?", new Object[]{apple});
        OrderItem item = orders.detail(customer1, id).items.get(0);
        assertEquals("Táo đỏ", item.fruitName);
        assertEquals(15000, item.unitPrice);
        assertNull(item.fruitId);
    }

    @Test public void customerSeesOnlyOwnOrdersAdminSeesAll() {
        long id = orders.place(customer1, Arrays.asList(new CartLine(fruit("Táo đỏ"), 1, 15000)));
        assertEquals(1, orders.list(customer1).size());
        assertEquals(0, orders.list(customer2).size());
        assertEquals(1, orders.list(admin).size());
        assertEquals(customer1, orders.detail(admin, id).customer.accountId);
        expect(ErrorCode.NOT_FOUND, () -> orders.detail(customer2, id));
    }

    @Test public void onlyAdminAdvancesStatusInOrderWithoutTouchingStock() {
        long id = orders.place(customer1, Arrays.asList(new CartLine(fruit("Táo đỏ"), 2, 15000)));
        expect(ErrorCode.FORBIDDEN, () -> orders.advanceStatus(customer1, id, OrderStatus.CONFIRMED));
        expect(ErrorCode.INVALID_STATUS, () -> orders.advanceStatus(admin, id, OrderStatus.COMPLETED));
        orders.advanceStatus(admin, id, OrderStatus.CONFIRMED);
        orders.advanceStatus(admin, id, OrderStatus.COMPLETED);
        expect(ErrorCode.INVALID_STATUS, () -> orders.advanceStatus(admin, id, OrderStatus.PENDING));
        assertEquals(OrderStatus.COMPLETED, orders.detail(customer1, id).order.status);
        assertEquals(38, stock("Táo đỏ"));
    }

    private long fruit(String name) { return single("SELECT id FROM Fruit WHERE name=?", name); }
    private long stock(String name) { return single("SELECT quantity FROM Fruit WHERE name=?", name); }
    private long count(String table) { return single("SELECT COUNT(*) FROM \"" + table + "\""); }

    private long single(String sql, String... args) {
        try (Cursor c = helper.getReadableDatabase().rawQuery(sql, args)) {
            assertTrue(c.moveToFirst());
            return c.getLong(0);
        }
    }

    private static void expect(ErrorCode code, Runnable action) {
        try { action.run(); fail("Expected " + code); }
        catch (DomainException error) { assertEquals(code, error.code); }
    }
}
