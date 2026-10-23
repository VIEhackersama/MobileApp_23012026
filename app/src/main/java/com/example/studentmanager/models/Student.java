package com.example.studentmanager.models;

public final class Student {
    public final long id;
    public final String code;
    public final String fullname;
    public final String email;
    public final long accountId;
    public final String username;

    public Student(long id, String code, String fullname, String email, long accountId, String username) {
        this.id = id;
        this.code = code;
        this.fullname = fullname;
        this.email = email;
        this.accountId = accountId;
        this.username = username;
    }

    @Override public String toString() { return code + " · " + fullname; }
}
