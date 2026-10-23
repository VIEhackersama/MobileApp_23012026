package com.example.studentmanager.models;

public final class Teacher {
    public final long id;
    public final String fullname;
    public final String email;
    public final long accountId;

    public Teacher(long id, String fullname, String email, long accountId) {
        this.id = id;
        this.fullname = fullname;
        this.email = email;
        this.accountId = accountId;
    }
}
