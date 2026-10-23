package com.example.studentmanager.dal;

import org.junit.Test;
import static org.junit.Assert.*;

public class PasswordHasherTest {
    @Test public void passwordRoundTripPreservesUnicodeAndWhitespace() {
        String password = "  Mật khẩu Nhật 日本  ";
        String hash = PasswordHasher.hash(password);
        assertTrue(PasswordHasher.verify(password, hash));
        assertFalse(PasswordHasher.verify(password.trim(), hash));
        assertFalse(PasswordHasher.verify("wrong-password", hash));
        assertFalse(hash.contains(password));
    }

    @Test public void independentSaltsProduceDifferentHashes() {
        String first = PasswordHasher.hash("teacher123");
        String second = PasswordHasher.hash("teacher123");
        assertNotEquals(first, second);
        assertTrue(PasswordHasher.verify("teacher123", first));
        assertTrue(PasswordHasher.verify("teacher123", second));
    }

    @Test public void malformedHashesAreRejected() {
        for (String value : new String[]{"", "plaintext", "a:b:c", "120000:xx:00", "1:00:00", "999999999:00:00"}) {
            assertFalse(PasswordHasher.verify("password", value));
        }
        assertFalse(PasswordHasher.verify("password", null));
        assertFalse(PasswordHasher.verify(null, "120000:00:00"));
    }
}
