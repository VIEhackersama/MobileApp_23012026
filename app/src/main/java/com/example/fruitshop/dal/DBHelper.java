package com.example.fruitshop.dal;

import android.content.ContentValues;
import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import com.example.fruitshop.core.BusinessClock;
import com.example.fruitshop.core.BusinessDates;

/** Schema owner: person 1. Teammates share this helper; never create another production database. */
public final class DBHelper extends SQLiteOpenHelper {
    public static final String DATABASE_NAME = "fruit_shop.db";
    public static final int DATABASE_VERSION = 1;
    private final BusinessClock clock;
    public DBHelper(Context context) { this(context, DATABASE_NAME, BusinessClock.system()); }
    public DBHelper(Context context, String name, BusinessClock clock) {
        super(context, name, null, DATABASE_VERSION); this.clock = clock;
    }
    @Override public void onConfigure(SQLiteDatabase db) {
        super.onConfigure(db); db.setForeignKeyConstraintsEnabled(true);
    }
    @Override public void onCreate(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE Account (id INTEGER PRIMARY KEY AUTOINCREMENT, "
                + "username TEXT NOT NULL COLLATE NOCASE UNIQUE CHECK(length(username) BETWEEN 3 AND 40), "
                + "password TEXT NOT NULL, role TEXT NOT NULL CHECK(role IN ('ADMIN','CUSTOMER')))");
        db.execSQL("CREATE TABLE Fruit (id INTEGER PRIMARY KEY AUTOINCREMENT, "
                + "name TEXT NOT NULL CHECK(length(trim(name))>0), category TEXT NOT NULL CHECK(length(trim(category))>0), "
                + "price INTEGER NOT NULL CHECK(typeof(price)='integer' AND price>0), "
                + "quantity INTEGER NOT NULL CHECK(typeof(quantity)='integer' AND quantity>=0), "
                + "expiryDate TEXT NOT NULL CHECK(length(expiryDate)=10 AND expiryDate GLOB '[0-9][0-9][0-9][0-9]-[0-9][0-9]-[0-9][0-9]'), "
                + "description TEXT NOT NULL, unit TEXT NOT NULL CHECK(unit IN ('KG','PIECE','PACK')))");
        db.execSQL("CREATE TRIGGER fruit_unit_immutable BEFORE UPDATE OF unit ON Fruit "
                + "WHEN NEW.unit<>OLD.unit BEGIN SELECT RAISE(ABORT,'Fruit.unit is immutable'); END");
        db.execSQL("CREATE TABLE Customer (id INTEGER PRIMARY KEY AUTOINCREMENT, "
                + "fullname TEXT NOT NULL CHECK(length(trim(fullname))>0), "
                + "phone TEXT NOT NULL, email TEXT NOT NULL, "
                + "accountId INTEGER NOT NULL UNIQUE REFERENCES Account(id) ON DELETE RESTRICT)");
        db.execSQL("CREATE TRIGGER customer_role_guard BEFORE INSERT ON Customer "
                + "WHEN (SELECT role FROM Account WHERE id=NEW.accountId)<>'CUSTOMER' "
                + "BEGIN SELECT RAISE(ABORT,'Customer requires CUSTOMER account'); END");
        db.execSQL("CREATE TRIGGER customer_update_role_guard BEFORE UPDATE OF accountId ON Customer "
                + "WHEN (SELECT role FROM Account WHERE id=NEW.accountId)<>'CUSTOMER' "
                + "BEGIN SELECT RAISE(ABORT,'Customer requires CUSTOMER account'); END");
        db.execSQL("CREATE TRIGGER account_role_immutable BEFORE UPDATE OF role ON Account "
                + "WHEN NEW.role<>OLD.role BEGIN SELECT RAISE(ABORT,'Account.role is immutable'); END");
        db.execSQL("CREATE TABLE \"Order\" (id INTEGER PRIMARY KEY AUTOINCREMENT, "
                + "customerId INTEGER NOT NULL REFERENCES Customer(id) ON DELETE RESTRICT, "
                + "orderDate INTEGER NOT NULL CHECK(typeof(orderDate)='integer' AND orderDate>=0), "
                + "totalAmount INTEGER NOT NULL CHECK(typeof(totalAmount)='integer' AND totalAmount>=0), "
                + "status TEXT NOT NULL CHECK(status IN ('PENDING','CONFIRMED','COMPLETED')))");
        db.execSQL("CREATE TABLE OrderItem (id INTEGER PRIMARY KEY AUTOINCREMENT, "
                + "orderId INTEGER NOT NULL REFERENCES \"Order\"(id) ON DELETE RESTRICT, "
                + "fruitId INTEGER REFERENCES Fruit(id) ON DELETE SET NULL, fruitName TEXT NOT NULL, "
                + "unit TEXT NOT NULL CHECK(unit IN ('KG','PIECE','PACK')), "
                + "unitPrice INTEGER NOT NULL CHECK(typeof(unitPrice)='integer' AND unitPrice>0), "
                + "quantity INTEGER NOT NULL CHECK(typeof(quantity)='integer' AND quantity>0), "
                + "lineTotal INTEGER NOT NULL CHECK(typeof(lineTotal)='integer' AND lineTotal>=0), UNIQUE(orderId,fruitId))");
        db.execSQL("CREATE INDEX idx_order_customer ON \"Order\"(customerId)");
        db.execSQL("CREATE INDEX idx_order_item_order ON OrderItem(orderId)");
        db.execSQL("CREATE INDEX idx_order_item_fruit ON OrderItem(fruitId)");
        seed(db); // SQLiteOpenHelper calls onCreate in its own transaction.
    }
    private void seed(SQLiteDatabase db) {
        account(db, "admin01", "admin123", "ADMIN");
        customer(db, "customer01", "Nguyễn Minh An", "0901234567", "minhan@example.com");
        customer(db, "customer02", "Trần Thu Hà", "+84912345678", "thuha@example.com");
        fruit(db, "Cam sành", "Cam quýt", 40000, 12000, 7, "Cam tươi bán theo cân.", "KG");
        fruit(db, "Táo đỏ", "Táo lê", 15000, 40, 10, "Giá theo từng quả.", "PIECE");
        fruit(db, "Trái cây cắt sẵn", "Ăn liền", 25000, 15, 1, "Một phần trái cây hỗn hợp.", "PACK");
        fruit(db, "Chuối chín", "Nhiệt đới", 30000, 3000, 0, "Hết hạn vào cuối ngày hôm nay.", "KG");
        fruit(db, "Dâu tây", "Quả mọng", 120000, 1000, -1, "Mẫu đã hết hạn để kiểm tra nhận diện.", "KG");
        fruit(db, "Dưa lưới", "Dưa", 45000, 0, 5, "Mẫu hết hàng.", "PIECE");
    }
    private static long account(SQLiteDatabase db, String username, String password, String role) {
        ContentValues values = new ContentValues();
        values.put("username", username); values.put("password", PasswordHasher.hash(password)); values.put("role", role);
        return db.insertOrThrow("Account", null, values);
    }
    private static void customer(SQLiteDatabase db, String username, String fullname, String phone, String email) {
        long accountId = account(db, username, "customer123", "CUSTOMER");
        ContentValues values = new ContentValues();
        values.put("fullname", fullname); values.put("phone", phone); values.put("email", email); values.put("accountId", accountId);
        db.insertOrThrow("Customer", null, values);
    }
    private void fruit(SQLiteDatabase db, String name, String category, long price, long quantity,
                       int days, String description, String unit) {
        ContentValues values = new ContentValues();
        values.put("name", name); values.put("category", category); values.put("price", price);
        values.put("quantity", quantity); values.put("expiryDate", BusinessDates.plusDays(clock, days));
        values.put("description", description); values.put("unit", unit);
        db.insertOrThrow("Fruit", null, values);
    }
    @Override public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        throw new IllegalStateException("Cần migration rõ ràng để bảo toàn dữ liệu Fruit Shop.");
    }
}
