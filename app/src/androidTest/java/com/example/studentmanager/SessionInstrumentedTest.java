package com.example.studentmanager;

import android.content.Context;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;
import com.example.studentmanager.models.Account;
import com.example.studentmanager.session.SessionManager;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import static org.junit.Assert.*;

@RunWith(AndroidJUnit4.class)
public class SessionInstrumentedTest {
    private Context context;
    @Before public void setup() {
        // Test APK context uses separate preferences from the actual application.
        context = InstrumentationRegistry.getInstrumentation().getContext();
        new SessionManager(context).signOut();
    }
    @After public void cleanup() { new SessionManager(context).signOut(); }

    private Account account() { return new Account(7, "demo", Account.STUDENT, "Demo", "d@test.com", "SV007"); }

    @Test public void rememberedSessionRestoresAndLogoutClearsEverything() {
        SessionManager session = new SessionManager(context);
        session.signIn(account(), true);
        assertEquals(7, new SessionManager(context).accountId());
        assertEquals("demo", context.getSharedPreferences(SessionManager.PREFERENCES, 0).getString("username", ""));
        assertFalse(context.getSharedPreferences(SessionManager.PREFERENCES, 0).contains("password"));
        session.signOut();
        assertEquals(-1, new SessionManager(context).accountId());
        assertTrue(context.getSharedPreferences(SessionManager.PREFERENCES, 0).getAll().isEmpty());
    }

    @Test public void nonRememberedSessionLivesInMemoryOnly() {
        SessionManager session = new SessionManager(context);
        session.signIn(account(), false);
        assertEquals(7, session.accountId());
        assertEquals(-1, new SessionManager(context).accountId());
        assertTrue(context.getSharedPreferences(SessionManager.PREFERENCES, 0).getAll().isEmpty());
    }

    @Test public void newNonRememberedLoginRemovesPreviousRememberedSession() {
        SessionManager session = new SessionManager(context);
        session.signIn(account(), true);
        session.signIn(new Account(8, "another", Account.TEACHER, "Teacher", "t@test.com", null), false);
        assertEquals(8, session.accountId());
        assertEquals(-1, new SessionManager(context).accountId());
    }
}
