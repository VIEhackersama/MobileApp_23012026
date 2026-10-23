package com.example.studentmanager.dal;

import android.content.ContentValues;
import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

public final class DBHelper extends SQLiteOpenHelper {
    public static final String DATABASE_NAME = "student_manager.db";

    public DBHelper(Context context) { this(context, DATABASE_NAME); }

    /** A separate database name lets instrumentation tests avoid touching real learning data. */
    public DBHelper(Context context, String name) { super(context, name, null, 1); }

    @Override public void onConfigure(SQLiteDatabase db) {
        super.onConfigure(db);
        db.setForeignKeyConstraintsEnabled(true);
    }

    @Override public void onCreate(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE Account (id INTEGER PRIMARY KEY AUTOINCREMENT, "
                + "username TEXT NOT NULL COLLATE NOCASE UNIQUE, password TEXT NOT NULL, "
                + "role TEXT NOT NULL CHECK(role IN ('TEACHER','STUDENT')))");
        db.execSQL("CREATE TABLE Student (id INTEGER PRIMARY KEY AUTOINCREMENT, "
                + "code TEXT NOT NULL COLLATE NOCASE UNIQUE, fullname TEXT NOT NULL, email TEXT NOT NULL, "
                + "accountId INTEGER NOT NULL UNIQUE REFERENCES Account(id) ON DELETE RESTRICT)");
        db.execSQL("CREATE TABLE Teacher (id INTEGER PRIMARY KEY AUTOINCREMENT, "
                + "fullname TEXT NOT NULL, email TEXT NOT NULL, "
                + "accountId INTEGER NOT NULL UNIQUE REFERENCES Account(id) ON DELETE RESTRICT)");
        db.execSQL("CREATE TABLE Score (id INTEGER PRIMARY KEY AUTOINCREMENT, "
                + "studentCode TEXT NOT NULL COLLATE NOCASE REFERENCES Student(code) ON DELETE RESTRICT, "
                + "teacherCode INTEGER NOT NULL REFERENCES Teacher(id) ON DELETE RESTRICT, "
                + "subject TEXT NOT NULL COLLATE NOCASE, score REAL NOT NULL CHECK(score >= 0 AND score <= 10), "
                + "UNIQUE(studentCode, teacherCode, subject))");
        db.execSQL("CREATE INDEX idx_score_teacher ON Score(teacherCode)");
        // SQLiteOpenHelper runs onCreate inside a transaction. Seed only here, never on app launch.
        long teacher1 = addTeacher(db, "teacher01", "Nguyễn Minh Anh", "minhanh@school.edu.vn");
        long teacher2 = addTeacher(db, "teacher02", "Trần Hoàng Nam", "hoangnam@school.edu.vn");
        addStudent(db, "SV001", "Lê Thu Hà", "thuha@student.edu.vn", "student01");
        addStudent(db, "SV002", "Phạm Đức Huy", "duchuy@student.edu.vn", "student02");
        addStudent(db, "SV003", "Võ Khánh Linh", "khanhlinh@student.edu.vn", "student03");
        addScore(db, "SV001", teacher1, "Thiết kế ứng dụng di động", 8.5);
        addScore(db, "SV002", teacher1, "Thiết kế ứng dụng di động", 7.5);
        addScore(db, "SV003", teacher1, "Thiết kế ứng dụng di động", 9.0);
        addScore(db, "SV001", teacher2, "Cơ sở dữ liệu", 8.0);
        addScore(db, "SV002", teacher2, "Cơ sở dữ liệu", 9.0);
        addScore(db, "SV003", teacher2, "Cơ sở dữ liệu", 8.5);
    }

    private static long addAccount(SQLiteDatabase db, String username, String password, String role) {
        ContentValues values = new ContentValues();
        values.put("username", username);
        values.put("password", PasswordHasher.hash(password));
        values.put("role", role);
        return db.insertOrThrow("Account", null, values);
    }

    private static long addTeacher(SQLiteDatabase db, String username, String name, String email) {
        ContentValues values = new ContentValues();
        values.put("fullname", name);
        values.put("email", email);
        values.put("accountId", addAccount(db, username, "teacher123", "TEACHER"));
        return db.insertOrThrow("Teacher", null, values);
    }

    private static void addStudent(SQLiteDatabase db, String code, String name, String email, String username) {
        ContentValues values = new ContentValues();
        values.put("code", code);
        values.put("fullname", name);
        values.put("email", email);
        values.put("accountId", addAccount(db, username, "student123", "STUDENT"));
        db.insertOrThrow("Student", null, values);
    }

    private static void addScore(SQLiteDatabase db, String code, long teacherId, String subject, double score) {
        ContentValues values = new ContentValues();
        values.put("studentCode", code);
        values.put("teacherCode", teacherId);
        values.put("subject", subject);
        values.put("score", score);
        db.insertOrThrow("Score", null, values);
    }

    @Override public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        // Never drop user data as a fallback. Add explicit migrations when changing DATABASE_VERSION.
        throw new IllegalStateException("Chưa có migration cho phiên bản cơ sở dữ liệu này.");
    }
}
