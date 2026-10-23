package com.example.fruitshop;

import android.content.Context;
import android.content.ContextWrapper;
import android.content.SharedPreferences;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;
import com.example.fruitshop.core.DomainException;
import com.example.fruitshop.core.ErrorCode;
import com.example.fruitshop.dal.AuthRepository;
import com.example.fruitshop.dal.DBHelper;
import com.example.fruitshop.core.BusinessClock;
import com.example.fruitshop.models.Account;
import com.example.fruitshop.models.Role;
import com.example.fruitshop.session.SessionManager;
import java.util.ArrayList;
import java.util.List;
import org.junit.*;
import org.junit.runner.RunWith;
import static org.junit.Assert.*;

@RunWith(AndroidJUnit4.class)
public class SessionInstrumentedTest {
    private Context context;
    @Before public void setup() {
        Context target = InstrumentationRegistry.getInstrumentation().getTargetContext();
        // Instrumentation runs with the target app UID. Isolate names within that writable sandbox.
        context = new ContextWrapper(target) {
            @Override public SharedPreferences getSharedPreferences(String name, int mode) {
                return super.getSharedPreferences("instrumentation_" + name, mode);
            }
        };
        new SessionManager(context).signOut();
    }
    @After public void cleanup() {
        new SessionManager(context).signOut(); context.getSharedPreferences("login_session", 0).edit().clear().commit();
    }
    private Account account() { return new Account(7, "demo", Role.CUSTOMER); }
    @Test public void rememberRestoresAndLogoutClearsPreferences() {
        SessionManager session = new SessionManager(context); session.signIn(account(), true);
        assertEquals(7, new SessionManager(context).accountId());
        assertFalse(context.getSharedPreferences(SessionManager.PREFERENCES, 0).contains("password"));
        session.signOut(); assertEquals(-1, new SessionManager(context).accountId());
        assertTrue(context.getSharedPreferences(SessionManager.PREFERENCES, 0).getAll().isEmpty());
    }
    @Test public void nonRememberedSessionIsMemoryOnlyAndClearsOldRememberedLogin() {
        SessionManager session = new SessionManager(context); session.signIn(account(), true); session.signIn(account(), false);
        assertEquals(7, session.accountId()); assertEquals(-1, new SessionManager(context).accountId());
    }
    @Test public void listenersObserveSwitchAndLogoutAndCanBeRemoved() {
        SessionManager session = new SessionManager(context);
        List<String> changes = new ArrayList<>();
        SessionManager.Listener listener = (previous, current) -> changes.add(previous + ":" + current);
        session.addListener(listener); session.addListener(listener);
        session.signIn(account(), false); session.signIn(new Account(8, "admin", Role.ADMIN), false); session.signOut();
        assertEquals(java.util.Arrays.asList("-1:7", "7:8", "8:-1"), changes);
        session.removeListener(listener); session.signOut(); assertEquals(3, changes.size());
    }
    @Test public void previousApplicationPreferencesCannotRestoreFruitShopSession() {
        context.getSharedPreferences("login_session", 0).edit().putBoolean("remember", true).putLong("accountId", 1).commit();
        assertEquals(-1, new SessionManager(context).accountId());
    }
    @Test public void forgedPreferenceRoleDoesNotGrantAdmin() {
        String databaseName = "session_role_test.db";
        context.deleteDatabase(databaseName);
        DBHelper helper = new DBHelper(context, databaseName, BusinessClock.system());
        try {
            AuthRepository auth = new AuthRepository(helper);
            Account customer = auth.login("customer01", "customer123");
            SessionManager session = new SessionManager(context); session.signIn(customer, true);
            context.getSharedPreferences(SessionManager.PREFERENCES, 0).edit().putString("role", "ADMIN").commit();
            long restoredId = new SessionManager(context).accountId();
            assertEquals(Role.CUSTOMER, auth.accountById(restoredId).role);
            try { auth.requireAdmin(restoredId); fail("Role escalation"); }
            catch (DomainException error) { assertEquals(ErrorCode.FORBIDDEN, error.code); }
        } finally { helper.close(); context.deleteDatabase(databaseName); }
    }
}
