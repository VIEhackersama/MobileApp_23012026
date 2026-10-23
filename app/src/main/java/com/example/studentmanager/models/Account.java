package com.example.studentmanager.models;

public final class Account {
    public static final String TEACHER = "TEACHER";
    public static final String STUDENT = "STUDENT";
    public final long id;
    public final String username;
    public final String role;
    public final String fullname;
    public final String email;
    public final String studentCode;

    public Account(long id, String username, String role, String fullname, String email, String studentCode) {
        this.id = id;
        this.username = username;
        this.role = role;
        this.fullname = fullname;
        this.email = email;
        this.studentCode = studentCode;
    }

    public boolean isTeacher() { return TEACHER.equals(role); }
}
