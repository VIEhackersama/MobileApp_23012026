package com.example.fruitshop.dal;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteConstraintException;
import android.database.sqlite.SQLiteDatabase;
import com.example.fruitshop.contracts.FruitDataSource;
import com.example.fruitshop.core.BusinessClock;
import com.example.fruitshop.core.BusinessDates;
import com.example.fruitshop.core.DomainException;
import com.example.fruitshop.core.ErrorCode;
import com.example.fruitshop.dto.FruitInput;
import com.example.fruitshop.models.Account;
import com.example.fruitshop.models.Fruit;
import com.example.fruitshop.models.Role;
import com.example.fruitshop.models.SaleUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/** Synchronous FruitDataSource; call it on FruitShopApp.databaseExecutor. */
public final class FruitRepository implements FruitDataSource {
    private final DBHelper helper;
    private final AuthRepository auth;
    private final BusinessClock clock;

    public FruitRepository(DBHelper helper, AuthRepository auth, BusinessClock clock) {
        this.helper = helper;
        this.auth = auth;
        this.clock = clock;
    }

    @Override public List<Fruit> list(long actorAccountId, String query) {
        Account actor = auth.accountById(actorAccountId);
        boolean customer = actor.role == Role.CUSTOMER;
        String search = query == null ? "" : query.trim().toLowerCase(Locale.ROOT);
        String today = BusinessDates.today(clock);
        List<Fruit> fruits = new ArrayList<>();
        String sql = "SELECT id,name,category,price,quantity,expiryDate,description,unit FROM Fruit"
                + (customer ? " WHERE quantity>0 AND expiryDate>=?" : "")
                + " ORDER BY name COLLATE NOCASE,id";
        try (Cursor cursor = helper.getReadableDatabase().rawQuery(sql, customer ? new String[]{today} : null)) {
            while (cursor.moveToNext()) {
                Fruit fruit = read(cursor);
                if (search.isEmpty() || fruit.name.toLowerCase(Locale.ROOT).contains(search)
                        || fruit.category.toLowerCase(Locale.ROOT).contains(search)) fruits.add(fruit);
            }
        }
        return fruits;
    }

    @Override public Fruit detail(long actorAccountId, long fruitId) {
        Account actor = auth.accountById(actorAccountId);
        Fruit fruit = find(helper.getReadableDatabase(), fruitId);
        if (actor.role == Role.CUSTOMER
                && (fruit.quantity == 0 || BusinessDates.isExpired(fruit.expiryDate, clock))) throw notFound();
        return fruit;
    }

    @Override public long create(long actorAccountId, FruitInput input) {
        auth.requireAdmin(actorAccountId);
        ContentValues values = values(input);
        SQLiteDatabase db = helper.getWritableDatabase();
        db.beginTransaction();
        try {
            long id = db.insertOrThrow("Fruit", null, values);
            db.setTransactionSuccessful();
            return id;
        } catch (SQLiteConstraintException error) {
            throw storage(error);
        } finally { db.endTransaction(); }
    }

    @Override public void update(long actorAccountId, long fruitId, FruitInput input) {
        auth.requireAdmin(actorAccountId);
        ContentValues values = values(input);
        SQLiteDatabase db = helper.getWritableDatabase();
        db.beginTransaction();
        try {
            Fruit previous = find(db, fruitId);
            if (previous.unit != input.unit) throw invalid("Không thể đổi đơn vị bán của mặt hàng đã tạo.");
            if (db.update("Fruit", values, "id=?", new String[]{Long.toString(fruitId)}) != 1) throw notFound();
            db.setTransactionSuccessful();
        } catch (SQLiteConstraintException error) {
            throw storage(error);
        } finally { db.endTransaction(); }
    }

    @Override public void delete(long actorAccountId, long fruitId) {
        auth.requireAdmin(actorAccountId);
        SQLiteDatabase db = helper.getWritableDatabase();
        db.beginTransaction();
        try {
            if (db.delete("Fruit", "id=?", new String[]{Long.toString(fruitId)}) != 1) throw notFound();
            db.setTransactionSuccessful();
        } catch (SQLiteConstraintException error) {
            throw storage(error);
        } finally { db.endTransaction(); }
    }

    private static ContentValues values(FruitInput input) {
        if (input == null) throw invalid("Vui lòng nhập thông tin trái cây.");
        String name = required(input.name, "Tên trái cây", 100);
        String category = required(input.category, "Danh mục", 80);
        String description = input.description == null ? "" : input.description.trim();
        if (description.length() > 1000) throw invalid("Mô tả tối đa 1000 ký tự.");
        if (input.price <= 0) throw invalid("Giá phải lớn hơn 0 đồng.");
        if (input.quantity < 0) throw invalid("Tồn kho không được âm.");
        if (input.unit == null) throw invalid("Vui lòng chọn đơn vị bán.");
        if (!BusinessDates.isValid(input.expiryDate)) throw invalid("Hạn dùng phải là ngày YYYY-MM-DD hợp lệ.");
        ContentValues values = new ContentValues();
        values.put("name", name); values.put("category", category); values.put("price", input.price);
        values.put("quantity", input.quantity); values.put("expiryDate", input.expiryDate);
        values.put("description", description); values.put("unit", input.unit.name());
        return values;
    }

    private static String required(String value, String label, int max) {
        String clean = value == null ? "" : value.trim();
        if (clean.isEmpty()) throw invalid("Vui lòng nhập " + label.toLowerCase(Locale.ROOT) + ".");
        if (clean.length() > max) throw invalid(label + " tối đa " + max + " ký tự.");
        return clean;
    }

    private static Fruit find(SQLiteDatabase db, long id) {
        try (Cursor cursor = db.rawQuery("SELECT id,name,category,price,quantity,expiryDate,description,unit FROM Fruit WHERE id=?",
                new String[]{Long.toString(id)})) {
            if (!cursor.moveToFirst()) throw notFound();
            return read(cursor);
        }
    }

    private static Fruit read(Cursor cursor) {
        return new Fruit(cursor.getLong(0), cursor.getString(1), cursor.getString(2), cursor.getLong(3),
                cursor.getLong(4), cursor.getString(5), cursor.getString(6), SaleUnit.valueOf(cursor.getString(7)));
    }

    private static DomainException invalid(String message) { return new DomainException(ErrorCode.INVALID_INPUT, message); }
    private static DomainException notFound() { return new DomainException(ErrorCode.NOT_FOUND, "Không tìm thấy trái cây."); }
    private static DomainException storage(Exception cause) {
        return new DomainException(ErrorCode.STORAGE_ERROR, "Không thể lưu thay đổi trái cây.", cause);
    }
}
