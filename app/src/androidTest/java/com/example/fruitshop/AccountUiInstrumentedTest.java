package com.example.fruitshop;

import android.app.Instrumentation;
import android.content.Intent;
import android.database.sqlite.SQLiteDatabase;
import androidx.test.core.app.ActivityScenario;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;
import com.example.fruitshop.activities.LoginActivity;
import org.junit.*;
import org.junit.runner.RunWith;
import java.util.concurrent.TimeUnit;
import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.Espresso.pressBackUnconditionally;
import static androidx.test.espresso.action.ViewActions.*;
import static androidx.test.espresso.assertion.ViewAssertions.*;
import static androidx.test.espresso.matcher.ViewMatchers.*;
import static org.hamcrest.Matchers.*;

@RunWith(AndroidJUnit4.class)
public class AccountUiInstrumentedTest {
    private ActivityScenario<LoginActivity> scenario;
    private FruitShopApp app;
    private String registrationUsername;
    @Before public void setup() {
        Instrumentation instrumentation = InstrumentationRegistry.getInstrumentation();
        app = (FruitShopApp) instrumentation.getTargetContext().getApplicationContext();
        instrumentation.runOnMainSync(() -> app.session.signOut());
        Intent intent = new Intent(app, LoginActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        scenario = ActivityScenario.launch(intent);
    }
    @After public void cleanup() throws Exception {
        scenario.close();
        InstrumentationRegistry.getInstrumentation().runOnMainSync(() -> app.session.signOut());
        if (registrationUsername != null) app.databaseExecutor.submit(() -> {
            SQLiteDatabase db = app.database.getWritableDatabase();
            db.beginTransaction();
            try {
                db.execSQL("DELETE FROM Customer WHERE accountId IN (SELECT id FROM Account WHERE username=?)", new Object[]{registrationUsername});
                db.delete("Account", "username=?", new String[]{registrationUsername}); db.setTransactionSuccessful();
            } finally { db.endTransaction(); }
        }).get(30, TimeUnit.SECONDS);
    }
    private void awaitDatabase() throws Exception {
        for (int i = 0; i < 4; i++) {
            app.databaseExecutor.submit(() -> {}).get(30, TimeUnit.SECONDS);
            InstrumentationRegistry.getInstrumentation().waitForIdleSync();
        }
    }
    private void fill(int id, String text) {
        onView(withId(id)).perform(scrollTo(), replaceText(text), closeSoftKeyboard());
    }
    private void login(String username, String password) throws Exception {
        fill(R.id.username, username); fill(R.id.password, password);
        onView(withId(R.id.login)).perform(scrollTo(), click()); awaitDatabase();
    }
    @Test public void badLoginShowsErrorAndAllowsRetry() throws Exception {
        login("admin01", "wrong");
        onView(withId(R.id.error)).check(matches(allOf(isDisplayed(), withText(containsString("không đúng")))));
        onView(withId(R.id.login)).check(matches(isEnabled()));
    }
    @Test public void adminHomeShowsCatalogAndLogout() throws Exception {
        login("admin01", "admin123");
        onView(withText(R.string.fruit_catalog)).check(matches(isDisplayed()));
        onView(withId(R.id.customer_catalog)).check(matches(withEffectiveVisibility(Visibility.GONE)));
        onView(withId(R.id.logout)).check(matches(isDisplayed()));
    }
    @Test public void registrationValidatesAndAutomaticallySignsInCustomer() throws Exception {
        registrationUsername = "ui_" + System.currentTimeMillis();
        onView(withId(R.id.open_register)).perform(scrollTo(), click());
        fill(R.id.fullname, "UI Customer"); fill(R.id.phone, "0912345678"); fill(R.id.email, "ui@example.com");
        fill(R.id.username, registrationUsername); fill(R.id.password, "abc123"); fill(R.id.confirm_password, "different");
        onView(withId(R.id.register)).perform(scrollTo(), click()); awaitDatabase();
        onView(withId(R.id.error)).check(matches(withText(containsString("chưa khớp"))));
        fill(R.id.confirm_password, "abc123");
        onView(withId(R.id.register)).perform(scrollTo(), click()); awaitDatabase();
        onView(withId(R.id.customer_catalog)).check(matches(isDisplayed()));
        com.example.fruitshop.models.Customer profile = app.databaseExecutor.submit(
                () -> app.auth.requireCustomer(app.session.accountId())).get(30, TimeUnit.SECONDS);
        org.junit.Assert.assertEquals("ui@example.com", profile.email);
        org.junit.Assert.assertFalse(app.getSharedPreferences("fruit_shop_session", 0).getBoolean("remember", false));
    }
    @Test public void logoutClearsBackStackAndCannotRestoreProtectedScreen() throws Exception {
        onView(withId(R.id.remember)).perform(scrollTo(), click());
        login("customer01", "customer123");
        onView(withId(R.id.customer_catalog)).check(matches(isDisplayed()));
        onView(withId(R.id.logout)).perform(scrollTo(), click());
        onView(withId(R.id.login)).check(matches(isDisplayed()));
        pressBackUnconditionally();
        Intent intent = new Intent(app, LoginActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        InstrumentationRegistry.getInstrumentation().runOnMainSync(() -> app.startActivity(intent));
        awaitDatabase(); onView(withId(R.id.login)).check(matches(isDisplayed()));
        org.junit.Assert.assertEquals(-1, app.session.accountId());
    }
}
