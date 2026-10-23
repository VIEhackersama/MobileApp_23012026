package com.example.fruitshop.dto;

/** No role field: public registration can only create a CUSTOMER. Never log this object. */
public final class RegistrationInput {
    public final String fullname, phone, email, username, password, confirmPassword;
    public RegistrationInput(String fullname, String phone, String email, String username,
                             String password, String confirmPassword) {
        this.fullname = fullname; this.phone = phone; this.email = email; this.username = username;
        this.password = password; this.confirmPassword = confirmPassword;
    }
}
