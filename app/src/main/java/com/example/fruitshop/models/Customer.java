package com.example.fruitshop.models;

public final class Customer {
    public final long id, accountId;
    public final String fullname, phone, email;
    public Customer(long id, String fullname, String phone, String email, long accountId) {
        this.id = id; this.fullname = fullname; this.phone = phone; this.email = email; this.accountId = accountId;
    }
}
