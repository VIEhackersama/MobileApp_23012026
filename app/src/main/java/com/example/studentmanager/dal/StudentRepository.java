package com.example.studentmanager.dal;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteConstraintException;
import android.database.sqlite.SQLiteDatabase;
import android.util.Patterns;
import com.example.studentmanager.models.Account;
import com.example.studentmanager.models.Score;
import com.example.studentmanager.models.Student;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/** All callers supply the signed-in account id; role/ownership comes from SQLite, never UI extras. */
public final class StudentRepository {
    private final DBHelper helper;

    public StudentRepository(DBHelper helper) { this.helper = helper; }

    public Account login(String username, String password) {
        String name = required(username, "Tên đăng nhập", 40);
        if (password == null || password.isEmpty()) throw new IllegalArgumentException("Vui lòng nhập mật khẩu.");
        try (Cursor c = helper.getReadableDatabase().rawQuery(
                "SELECT id,password FROM Account WHERE username=?", new String[]{name})) {
            if (!c.moveToFirst() || !PasswordHasher.verify(password, c.getString(1))) {
                throw new IllegalArgumentException("Tên đăng nhập hoặc mật khẩu không đúng.");
            }
            return accountById(c.getLong(0));
        }
    }

    public Account accountById(long id) {
        try (Cursor c = helper.getReadableDatabase().rawQuery(
                "SELECT a.id,a.username,a.role,COALESCE(t.fullname,s.fullname),"
                        + "COALESCE(t.email,s.email),s.code FROM Account a "
                        + "LEFT JOIN Teacher t ON t.accountId=a.id AND a.role='TEACHER' "
                        + "LEFT JOIN Student s ON s.accountId=a.id AND a.role='STUDENT' WHERE a.id=?",
                new String[]{Long.toString(id)})) {
            if (!c.moveToFirst() || c.isNull(3)) throw new SecurityException("Phiên đăng nhập không còn hợp lệ.");
            return new Account(c.getLong(0), c.getString(1), c.getString(2), c.getString(3), c.getString(4), c.getString(5));
        }
    }

    private long requireTeacher(SQLiteDatabase db, long actorId) {
        try (Cursor c = db.rawQuery("SELECT t.id FROM Teacher t JOIN Account a ON a.id=t.accountId "
                + "WHERE a.id=? AND a.role='TEACHER'", new String[]{Long.toString(actorId)})) {
            if (!c.moveToFirst()) throw new SecurityException("Chức năng này chỉ dành cho giáo viên.");
            return c.getLong(0);
        }
    }

    public List<Student> students(long actorId) {
        SQLiteDatabase db = helper.getReadableDatabase();
        requireTeacher(db, actorId);
        List<Student> result = new ArrayList<>();
        try (Cursor c = db.rawQuery("SELECT s.id,s.code,s.fullname,s.email,s.accountId,a.username "
                + "FROM Student s JOIN Account a ON a.id=s.accountId ORDER BY s.code", null)) {
            while (c.moveToNext()) result.add(new Student(c.getLong(0), c.getString(1), c.getString(2),
                    c.getString(3), c.getLong(4), c.getString(5)));
        }
        return result;
    }

    public List<Score> scores(long actorId) {
        Account actor = accountById(actorId);
        List<Score> result = new ArrayList<>();
        String condition = actor.isTeacher() ? "t.accountId=?" : "s.accountId=?";
        try (Cursor c = helper.getReadableDatabase().rawQuery(
                "SELECT sc.id,sc.studentCode,sc.teacherCode,sc.subject,sc.score,s.fullname,t.fullname "
                        + "FROM Score sc JOIN Student s ON s.code=sc.studentCode "
                        + "JOIN Teacher t ON t.id=sc.teacherCode WHERE " + condition
                        + " ORDER BY sc.subject,sc.studentCode", new String[]{Long.toString(actorId)})) {
            while (c.moveToNext()) result.add(new Score(c.getLong(0), c.getString(1), c.getLong(2),
                    c.getString(3), c.getDouble(4), c.getString(5), c.getString(6)));
        }
        return result;
    }

    public void createStudent(long actorId, String code, String name, String email, String username, String password) {
        String cleanCode = required(code, "Mã sinh viên", 30).toUpperCase(Locale.ROOT);
        ContentValues profile = profile(name, email);
        String cleanUsername = required(username, "Tên đăng nhập", 40);
        if (cleanUsername.length() < 3 || cleanUsername.matches(".*\\s.*")) {
            throw new IllegalArgumentException("Tên đăng nhập cần ít nhất 3 ký tự và không có khoảng trắng.");
        }
        if (password == null || password.length() < 6 || password.length() > 128) {
            throw new IllegalArgumentException("Mật khẩu cần từ 6 đến 128 ký tự.");
        }
        SQLiteDatabase db = helper.getWritableDatabase();
        db.beginTransaction();
        try {
            requireTeacher(db, actorId);
            ContentValues account = new ContentValues();
            account.put("username", cleanUsername);
            account.put("password", PasswordHasher.hash(password));
            account.put("role", Account.STUDENT);
            long accountId = db.insertOrThrow("Account", null, account);
            profile.put("code", cleanCode);
            profile.put("accountId", accountId);
            db.insertOrThrow("Student", null, profile);
            db.setTransactionSuccessful();
        } catch (SQLiteConstraintException e) {
            throw new IllegalArgumentException("Mã sinh viên hoặc tên đăng nhập đã tồn tại.", e);
        } finally { db.endTransaction(); }
    }

    public void updateStudent(long actorId, long studentId, String name, String email) {
        ContentValues values = profile(name, email);
        SQLiteDatabase db = helper.getWritableDatabase();
        db.beginTransaction();
        try {
            requireTeacher(db, actorId);
            if (db.update("Student", values, "id=?", new String[]{Long.toString(studentId)}) != 1) {
                throw new IllegalArgumentException("Sinh viên không còn tồn tại.");
            }
            db.setTransactionSuccessful();
        } finally { db.endTransaction(); }
    }

    public void deleteStudent(long actorId, long studentId) {
        SQLiteDatabase db = helper.getWritableDatabase();
        db.beginTransaction();
        try {
            requireTeacher(db, actorId);
            long accountId;
            String code;
            try (Cursor c = db.rawQuery("SELECT accountId,code FROM Student WHERE id=?", new String[]{Long.toString(studentId)})) {
                if (!c.moveToFirst()) throw new IllegalArgumentException("Sinh viên không còn tồn tại.");
                accountId = c.getLong(0);
                code = c.getString(1);
            }
            try (Cursor c = db.rawQuery("SELECT 1 FROM Score WHERE studentCode=? LIMIT 1", new String[]{code})) {
                if (c.moveToFirst()) throw new IllegalArgumentException("Không thể xóa sinh viên đã có điểm. Hãy xử lý điểm trước.");
            }
            db.delete("Student", "id=?", new String[]{Long.toString(studentId)});
            db.delete("Account", "id=?", new String[]{Long.toString(accountId)});
            db.setTransactionSuccessful();
        } finally { db.endTransaction(); }
    }

    /** id == 0 inserts; otherwise only rows belonging to the current teacher can be updated. */
    public void saveScore(long actorId, long id, String studentCode, String subject, double score) {
        String cleanCode = required(studentCode, "Sinh viên", 30);
        String cleanSubject = required(subject, "Môn học", 80);
        if (Double.isNaN(score) || Double.isInfinite(score) || score < 0 || score > 10) {
            throw new IllegalArgumentException("Điểm phải là số trong khoảng từ 0 đến 10.");
        }
        SQLiteDatabase db = helper.getWritableDatabase();
        db.beginTransaction();
        try {
            long teacherId = requireTeacher(db, actorId);
            if (id != 0) requireOwnedScore(db, id, teacherId);
            try (Cursor c = db.rawQuery("SELECT 1 FROM Student WHERE code=?", new String[]{cleanCode})) {
                if (!c.moveToFirst()) throw new IllegalArgumentException("Sinh viên không còn tồn tại.");
            }
            ContentValues values = new ContentValues();
            values.put("studentCode", cleanCode);
            values.put("teacherCode", teacherId);
            values.put("subject", cleanSubject);
            values.put("score", score);
            if (id == 0) db.insertOrThrow("Score", null, values);
            else db.update("Score", values, "id=? AND teacherCode=?", new String[]{Long.toString(id), Long.toString(teacherId)});
            db.setTransactionSuccessful();
        } catch (SQLiteConstraintException e) {
            throw new IllegalArgumentException("Bạn đã nhập điểm môn này cho sinh viên. Hãy sửa bản ghi hiện có.", e);
        } finally { db.endTransaction(); }
    }

    public void deleteScore(long actorId, long id) {
        SQLiteDatabase db = helper.getWritableDatabase();
        db.beginTransaction();
        try {
            long teacherId = requireTeacher(db, actorId);
            requireOwnedScore(db, id, teacherId);
            db.delete("Score", "id=? AND teacherCode=?", new String[]{Long.toString(id), Long.toString(teacherId)});
            db.setTransactionSuccessful();
        } finally { db.endTransaction(); }
    }

    private static void requireOwnedScore(SQLiteDatabase db, long id, long teacherId) {
        try (Cursor c = db.rawQuery("SELECT 1 FROM Score WHERE id=? AND teacherCode=?",
                new String[]{Long.toString(id), Long.toString(teacherId)})) {
            if (!c.moveToFirst()) throw new SecurityException("Bạn chỉ được sửa hoặc xóa điểm do mình nhập.");
        }
    }

    private static ContentValues profile(String name, String email) {
        String cleanEmail = required(email, "Email", 120);
        if (!Patterns.EMAIL_ADDRESS.matcher(cleanEmail).matches()) throw new IllegalArgumentException("Email không hợp lệ.");
        ContentValues values = new ContentValues();
        values.put("fullname", required(name, "Họ và tên", 100));
        values.put("email", cleanEmail);
        return values;
    }

    private static String required(String value, String label, int max) {
        if (value == null || value.trim().isEmpty()) throw new IllegalArgumentException("Vui lòng nhập " + label.toLowerCase(Locale.ROOT) + ".");
        String clean = value.trim();
        if (clean.length() > max) throw new IllegalArgumentException(label + " tối đa " + max + " ký tự.");
        return clean;
    }
}
