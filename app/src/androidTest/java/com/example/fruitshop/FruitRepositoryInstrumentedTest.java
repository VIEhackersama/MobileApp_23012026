package com.example.fruitshop;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;
import com.example.fruitshop.core.BusinessClock;
import com.example.fruitshop.core.BusinessDates;
import com.example.fruitshop.core.DomainException;
import com.example.fruitshop.core.ErrorCode;
import com.example.fruitshop.dal.AuthRepository;
import com.example.fruitshop.dal.DBHelper;
import com.example.fruitshop.dal.FruitRepository;
import com.example.fruitshop.dto.FruitInput;
import com.example.fruitshop.models.Fruit;
import com.example.fruitshop.models.SaleUnit;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import static org.junit.Assert.*;

@RunWith(AndroidJUnit4.class)
public class FruitRepositoryInstrumentedTest {
    private static final String TEST_DB = "fruit_repository_instrumented.db";
    private Context context;
    private DBHelper helper;
    private FruitRepository fruits;
    private AuthRepository auth;
    private BusinessClock clock;
    private long adminId, customerId;

    @Before public void setup() {
        context = InstrumentationRegistry.getInstrumentation().getTargetContext();
        context.deleteDatabase(TEST_DB);
        clock = BusinessClock.fixed(0, "UTC");
        helper = new DBHelper(context, TEST_DB, clock);
        auth = new AuthRepository(helper);
        fruits = new FruitRepository(helper, auth, clock);
        adminId = auth.login("admin01", "admin123").id;
        customerId = auth.login("customer01", "customer123").id;
    }

    @After public void cleanup() { helper.close(); context.deleteDatabase(TEST_DB); }

    @Test public void adminCrudSearchAndUnitRules() {
        String tomorrow = BusinessDates.plusDays(clock, 1);
        long id = fruits.create(adminId, new FruitInput("Xoài cát", "Nhiệt đới", 40000, 1250,
                tomorrow, "Chín cây", SaleUnit.KG));
        assertEquals(1, fruits.list(customerId, "XOÀI").size());
        assertEquals(1, fruits.list(adminId, "nhiệt đới").stream().filter(f -> f.id == id).count());
        assertEquals(1250, fruits.detail(customerId, id).quantity);
        fruits.update(adminId, id, new FruitInput("Xoài", "Nhiệt đới", 42000, 0,
                tomorrow, "", SaleUnit.KG));
        assertEquals(0, fruits.detail(adminId, id).quantity);
        expect(ErrorCode.NOT_FOUND, () -> fruits.detail(customerId, id));
        expect(ErrorCode.INVALID_INPUT, () -> fruits.update(adminId, id, new FruitInput("Xoài", "Nhiệt đới", 1, 1,
                tomorrow, "", SaleUnit.PIECE)));
        fruits.delete(adminId, id);
        expect(ErrorCode.NOT_FOUND, () -> fruits.detail(adminId, id));
    }

    @Test public void customerOnlySeesSaleableFruitAndCannotWrite() {
        String yesterday = BusinessDates.plusDays(clock, -1);
        long expiredId = fruits.create(adminId, new FruitInput("Mận", "Quả", 20000, 4,
                yesterday, "", SaleUnit.PIECE));
        assertEquals(1, fruits.list(adminId, "Mận").size());
        assertTrue(fruits.list(customerId, "Mận").isEmpty());
        expect(ErrorCode.NOT_FOUND, () -> fruits.detail(customerId, expiredId));
        expect(ErrorCode.FORBIDDEN, () -> fruits.create(customerId,
                new FruitInput("Lê", "Quả", 10000, 1, BusinessDates.today(clock), "", SaleUnit.PIECE)));
        expect(ErrorCode.FORBIDDEN, () -> fruits.delete(customerId, expiredId));
        expect(ErrorCode.INVALID_INPUT, () -> fruits.create(adminId,
                new FruitInput("Lê", "Quả", -1, 1, BusinessDates.today(clock), "", SaleUnit.PIECE)));
        expect(ErrorCode.INVALID_INPUT, () -> fruits.create(adminId,
                new FruitInput("Lê", "Quả", 1, -1, BusinessDates.today(clock), "", SaleUnit.PIECE)));
        expect(ErrorCode.INVALID_INPUT, () -> fruits.create(adminId,
                new FruitInput("Lê", "Quả", 1, 1, "2026-02-30", "", SaleUnit.PIECE)));
    }

    @Test public void deletingFruitKeepsHistoricalOrderSnapshot() {
        long id = fruits.create(adminId, new FruitInput("Lê", "Quả", 15000, 10,
                BusinessDates.plusDays(clock, 1), "", SaleUnit.PIECE));
        SQLiteDatabase db = helper.getWritableDatabase();
        long customerProfileId;
        try (Cursor cursor = db.rawQuery("SELECT id FROM Customer WHERE accountId=?",
                new String[]{Long.toString(customerId)})) {
            assertTrue(cursor.moveToFirst()); customerProfileId = cursor.getLong(0);
        }
        ContentValues order = new ContentValues();
        order.put("customerId", customerProfileId); order.put("orderDate", 0);
        order.put("totalAmount", 15000); order.put("status", "PENDING");
        long orderId = db.insertOrThrow("\"Order\"", null, order);
        ContentValues item = new ContentValues();
        item.put("orderId", orderId); item.put("fruitId", id); item.put("fruitName", "Lê");
        item.put("unit", "PIECE"); item.put("unitPrice", 15000); item.put("quantity", 1);
        item.put("lineTotal", 15000);
        db.insertOrThrow("OrderItem", null, item);
        fruits.delete(adminId, id);
        try (Cursor cursor = db.rawQuery("SELECT fruitId,fruitName,unitPrice,quantity,lineTotal FROM OrderItem WHERE orderId=?",
                new String[]{Long.toString(orderId)})) {
            assertTrue(cursor.moveToFirst());
            assertTrue(cursor.isNull(0)); assertEquals("Lê", cursor.getString(1));
            assertEquals(15000, cursor.getLong(2)); assertEquals(1, cursor.getLong(3));
            assertEquals(15000, cursor.getLong(4));
        }
    }

    private static void expect(ErrorCode code, Runnable action) {
        try { action.run(); fail("Expected " + code); }
        catch (DomainException error) { assertEquals(code, error.code); }
    }
}
