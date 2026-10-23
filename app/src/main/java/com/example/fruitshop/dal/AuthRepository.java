package com.example.fruitshop.dal;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteConstraintException;
import android.database.sqlite.SQLiteDatabase;
import android.util.Patterns;
import com.example.fruitshop.core.DomainException;
import com.example.fruitshop.core.ErrorCode;
import com.example.fruitshop.dto.RegistrationInput;
import com.example.fruitshop.models.Account;
import com.example.fruitshop.models.Customer;
import com.example.fruitshop.models.Role;

/** Synchronous; run on FruitShopApp.databaseExecutor. Never authorize from SharedPreferences.role. */
public final class AuthRepository {
    private final DBHelper helper;
    public AuthRepository(DBHelper helper) { this.helper = helper; }

    public Account login(String username, String password) {
        if (username == null || password == null || password.isEmpty() || password.length() > 128) throw authFailed();
        try (Cursor cursor = helper.getReadableDatabase().rawQuery("SELECT id,password FROM Account WHERE username=?",
                new String[]{username.trim()})) {
            if (!cursor.moveToFirst() || !PasswordHasher.verify(password, cursor.getString(1))) throw authFailed();
            return accountById(cursor.getLong(0));
        }
    }

    public Account accountById(long accountId) {
        try (Cursor cursor = helper.getReadableDatabase().rawQuery("SELECT a.id,a.username,a.role FROM Account a "
                + "WHERE a.id=? AND (a.role='ADMIN' OR EXISTS(SELECT 1 FROM Customer c WHERE c.accountId=a.id))",
                new String[]{Long.toString(accountId)})) {
            if (!cursor.moveToFirst()) throw new DomainException(ErrorCode.SESSION_EXPIRED, "Phiên đăng nhập không còn hợp lệ.");
            return new Account(cursor.getLong(0), cursor.getString(1), Role.valueOf(cursor.getString(2)));
        }
    }

    public Account requireAdmin(long actorAccountId) {
        Account account = accountById(actorAccountId);
        if (account.role != Role.ADMIN) throw new DomainException(ErrorCode.FORBIDDEN, "Chức năng này chỉ dành cho Admin.");
        return account;
    }

    public Customer requireCustomer(long actorAccountId) {
        if (accountById(actorAccountId).role != Role.CUSTOMER) {
            throw new DomainException(ErrorCode.FORBIDDEN, "Chức năng này chỉ dành cho khách hàng.");
        }
        try (Cursor cursor = helper.getReadableDatabase().rawQuery(
                "SELECT id,fullname,phone,email,accountId FROM Customer WHERE accountId=?",
                new String[]{Long.toString(actorAccountId)})) {
            if (!cursor.moveToFirst()) throw new DomainException(ErrorCode.SESSION_EXPIRED, "Không tìm thấy hồ sơ khách hàng.");
            return new Customer(cursor.getLong(0), cursor.getString(1), cursor.getString(2), cursor.getString(3), cursor.getLong(4));
        }
    }

    public Account registerCustomer(RegistrationInput input) {
        if (input == null) throw invalid("Vui lòng nhập thông tin đăng ký.");
        String username = required(input.username, "Tên đăng nhập", 40);
        if (!username.matches("[A-Za-z0-9._-]{3,40}")) throw invalid("Tên đăng nhập gồm 3–40 chữ Latin, số, dấu chấm, gạch ngang hoặc gạch dưới.");
        String fullname = required(input.fullname, "Họ và tên", 100);
        String email = required(input.email, "Email", 120);
        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) throw invalid("Email không hợp lệ.");
        String phone = required(input.phone, "Điện thoại", 50).replaceAll("[\\s-]", "");
        if (!phone.matches("\\+?[0-9]{8,15}")) throw invalid("Điện thoại cần 8–15 chữ số, có thể bắt đầu bằng +.");
        if (input.password == null || input.password.length() < 6 || input.password.length() > 128) {
            throw invalid("Mật khẩu cần từ 6 đến 128 ký tự.");
        }
        if (!input.password.equals(input.confirmPassword)) throw invalid("Mật khẩu xác nhận chưa khớp.");
        String hash = PasswordHasher.hash(input.password);
        SQLiteDatabase db = helper.getWritableDatabase();
        db.beginTransaction();
        long id;
        try {
            ContentValues account = new ContentValues();
            account.put("username", username); account.put("password", hash); account.put("role", Role.CUSTOMER.name());
            id = db.insertOrThrow("Account", null, account);
            ContentValues customer = new ContentValues();
            customer.put("fullname", fullname); customer.put("phone", phone); customer.put("email", email); customer.put("accountId", id);
            db.insertOrThrow("Customer", null, customer);
            db.setTransactionSuccessful();
        } catch (SQLiteConstraintException error) {
            if (error.getMessage() != null && error.getMessage().contains("Account.username")) {
                throw new DomainException(ErrorCode.USERNAME_EXISTS, "Tên đăng nhập đã tồn tại.", error);
            }
            throw new DomainException(ErrorCode.STORAGE_ERROR, "Không thể tạo tài khoản. Vui lòng thử lại.", error);
        } finally { db.endTransaction(); }
        return accountById(id);
    }
    private static String required(String value, String label, int max) {
        if (value == null || value.trim().isEmpty()) throw invalid("Vui lòng nhập " + label + ".");
        String result = value.trim();
        if (result.length() > max) throw invalid(label + " tối đa " + max + " ký tự.");
        return result;
    }
    private static DomainException invalid(String message) { return new DomainException(ErrorCode.INVALID_INPUT, message); }
    private static DomainException authFailed() { return new DomainException(ErrorCode.AUTH_FAILED, "Tên đăng nhập hoặc mật khẩu không đúng."); }
}
