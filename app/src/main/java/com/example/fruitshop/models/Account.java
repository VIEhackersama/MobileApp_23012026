package com.example.fruitshop.models;

/** Public account projection. The password hash is never returned to the UI. */
public final class Account {
    public final long id;
    public final String username;
    public final Role role;
    public Account(long id, String username, Role role) {
        this.id = id; this.username = username; this.role = role;
    }
}
