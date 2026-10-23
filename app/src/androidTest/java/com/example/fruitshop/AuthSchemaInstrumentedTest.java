package com.example.fruitshop;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteConstraintException;
import android.database.sqlite.SQLiteDatabase;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;
import com.example.fruitshop.core.BusinessClock;
import com.example.fruitshop.core.DomainException;
import com.example.fruitshop.core.ErrorCode;
import com.example.fruitshop.dal.AuthRepository;
import com.example.fruitshop.dal.DBHelper;
import com.example.fruitshop.dto.RegistrationInput;
import com.example.fruitshop.models.Account;
import com.example.fruitshop.models.Customer;
import com.example.fruitshop.models.Role;
import org.junit.*;
import org.junit.runner.RunWith;
import static org.junit.Assert.*;

@RunWith(AndroidJUnit4.class)
public class AuthSchemaInstrumentedTest {
    private static final String NAME = "foundation_test.db";
    private Context context;
    private DBHelper helper;
    private AuthRepository auth;
    @Before public void setup() {
        context = InstrumentationRegistry.getInstrumentation().getTargetContext();
        context.deleteDatabase(NAME);
        helper = new DBHelper(context, NAME, BusinessClock.fixed(1792717200000L, "Asia/Bangkok"));
        auth = new AuthRepository(helper);
    }
    @After public void cleanup() { helper.close(); context.deleteDatabase(NAME); }
    private RegistrationInput input(String username) {
        return new RegistrationInput("  New Customer  ", "+84 912-345-678", "new@example.com", username, "  abc123  ", "  abc123  ");
    }
    private long count(String table) {
        try (Cursor c = helper.getReadableDatabase().rawQuery("SELECT COUNT(*) FROM \"" + table + "\"", null)) {
            assertTrue(c.moveToFirst()); return c.getLong(0);
        }
    }
    @Test public void seedContainsFiveTablesAndNoOrders() {
        assertEquals(3, count("Account")); assertEquals(2, count("Customer"));
        assertEquals(6, count("Fruit")); assertEquals(0, count("Order")); assertEquals(0, count("OrderItem"));
        try (Cursor c = helper.getReadableDatabase().rawQuery(
                "SELECT COUNT(*) FROM sqlite_master WHERE type='table' AND name IN ('Account','Fruit','Customer','Order','OrderItem')", null)) {
            assertTrue(c.moveToFirst()); assertEquals(5, c.getInt(0));
        }
        try (Cursor c = helper.getReadableDatabase().rawQuery("PRAGMA foreign_keys", null)) {
            assertTrue(c.moveToFirst()); assertEquals(1, c.getInt(0));
        }
    }
    @Test public void adminDoesNotNeedCustomerProfileAndRolesComeFromDatabase() {
        Account admin = auth.login("ADMIN01", "admin123"); assertEquals(Role.ADMIN, admin.role);
        assertEquals(admin.id, auth.requireAdmin(admin.id).id);
        expect(ErrorCode.FORBIDDEN, () -> auth.requireCustomer(admin.id));
        Account customer = auth.login("customer01", "customer123");
        expect(ErrorCode.FORBIDDEN, () -> auth.requireAdmin(customer.id));
        expect(ErrorCode.SESSION_EXPIRED, () -> auth.accountById(-1));
        expect(ErrorCode.AUTH_FAILED, () -> auth.login("admin01", "wrong"));
        expect(ErrorCode.AUTH_FAILED, () -> auth.login("' OR 1=1 --", "wrong"));
    }
    @Test public void registrationNormalizesProfileAndPreservesPassword() {
        Account result = auth.registerCustomer(input("new_customer"));
        assertEquals(Role.CUSTOMER, result.role);
        Customer profile = auth.requireCustomer(result.id);
        assertEquals("New Customer", profile.fullname); assertEquals("+84912345678", profile.phone);
        assertEquals(result.id, auth.login("NEW_CUSTOMER", "  abc123  ").id);
        expect(ErrorCode.AUTH_FAILED, () -> auth.login("new_customer", "abc123"));
        assertEquals(4, count("Account")); assertEquals(3, count("Customer"));
    }
    @Test public void duplicateUsernameIsCaseInsensitiveAndAtomic() {
        expect(ErrorCode.USERNAME_EXISTS, () -> auth.registerCustomer(input("CUSTOMER01")));
        assertEquals(3, count("Account")); assertEquals(2, count("Customer"));
    }
    @Test public void profileInsertFailureRollsBackAccount() {
        helper.getWritableDatabase().execSQL("CREATE TEMP TRIGGER fail_profile BEFORE INSERT ON Customer BEGIN SELECT RAISE(ABORT,'test profile failure'); END");
        expect(ErrorCode.STORAGE_ERROR, () -> auth.registerCustomer(input("rollback_user")));
        assertEquals(3, count("Account")); assertEquals(2, count("Customer"));
        expect(ErrorCode.AUTH_FAILED, () -> auth.login("rollback_user", "  abc123  "));
    }
    @Test public void validationRejectsBadInputWithoutWrites() {
        expect(ErrorCode.INVALID_INPUT, () -> auth.registerCustomer(input("a b")));
        expect(ErrorCode.INVALID_INPUT, () -> auth.registerCustomer(input("ab")));
        expect(ErrorCode.INVALID_INPUT, () -> auth.registerCustomer(new RegistrationInput("Name", "123", "a@example.com", "valid", "abc123", "abc123")));
        expect(ErrorCode.INVALID_INPUT, () -> auth.registerCustomer(new RegistrationInput("Name", "0912345678", "bad", "valid", "abc123", "abc123")));
        expect(ErrorCode.INVALID_INPUT, () -> auth.registerCustomer(new RegistrationInput("Name", "0912345678", "a@example.com", "valid", "abc123", "mismatch")));
        expect(ErrorCode.INVALID_INPUT, () -> auth.registerCustomer(new RegistrationInput("", "0912345678", "a@example.com", "valid", "abc123", "abc123")));
        assertEquals(3, count("Account"));
    }
    @Test public void reopenDoesNotReseedOrLoseData() {
        Account registered = auth.registerCustomer(input("persistent"));
        helper.close(); helper = new DBHelper(context, NAME, BusinessClock.system()); auth = new AuthRepository(helper);
        assertEquals(registered.id, auth.login("persistent", "  abc123  ").id);
        assertEquals(4, count("Account")); assertEquals(6, count("Fruit"));
    }
    @Test public void schemaRejectsInvalidQuantitiesRolesAndForeignKeys() {
        SQLiteDatabase db = helper.getWritableDatabase();
        constraint(() -> db.execSQL("UPDATE Fruit SET quantity=-1 WHERE id=1"));
        constraint(() -> db.execSQL("UPDATE Fruit SET price=1.5 WHERE id=1"));
        constraint(() -> db.execSQL("UPDATE Fruit SET unit='PIECE' WHERE id=1"));
        constraint(() -> db.execSQL("UPDATE Account SET role='ADMIN' WHERE username='customer01'"));
        constraint(() -> db.execSQL("INSERT INTO Customer(fullname,phone,email,accountId) VALUES('x','0912345678','x@example.com',9999)"));
        constraint(() -> db.execSQL("INSERT INTO \"Order\"(customerId,orderDate,totalAmount,status) VALUES(1,1,1,'BAD')"));
    }
    @Test public void schemaPreservesOrderSnapshotsWhenFruitIsDeleted() {
        // Direct fixtures validate schema only; this is not an implementation of OrderDataSource.
        SQLiteDatabase db = helper.getWritableDatabase();
        db.execSQL("INSERT INTO \"Order\"(customerId,orderDate,totalAmount,status) VALUES(1,1,20000,'PENDING')");
        db.execSQL("INSERT INTO OrderItem(orderId,fruitId,fruitName,unit,unitPrice,quantity,lineTotal) VALUES(1,1,'Original name','KG',40000,500,20000)");
        db.execSQL("DELETE FROM Fruit WHERE id=1");
        try (Cursor c = db.rawQuery("SELECT fruitId,fruitName,unitPrice,quantity,lineTotal FROM OrderItem", null)) {
            assertTrue(c.moveToFirst()); assertTrue(c.isNull(0)); assertEquals("Original name", c.getString(1));
            assertEquals(40000, c.getLong(2)); assertEquals(500, c.getLong(3)); assertEquals(20000, c.getLong(4));
        }
        try (Cursor c = db.rawQuery("PRAGMA foreign_key_check", null)) { assertFalse(c.moveToFirst()); }
    }
    @Test public void passwordsAreStoredAsSaltedHashes() {
        auth.login("admin01", "admin123");
        try (Cursor c = helper.getReadableDatabase().rawQuery("SELECT password FROM Account WHERE username='admin01'", null)) {
            assertTrue(c.moveToFirst()); assertNotEquals("admin123", c.getString(0)); assertTrue(c.getString(0).startsWith("120000:"));
        }
    }
    private static void constraint(Runnable work) {
        try { work.run(); fail("Expected constraint failure"); } catch (SQLiteConstraintException expected) { }
    }
    private static void expect(ErrorCode code, Runnable work) {
        try { work.run(); fail("Expected " + code); } catch (DomainException error) { assertEquals(code, error.code); }
    }
}
