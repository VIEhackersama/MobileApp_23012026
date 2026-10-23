package com.example.fruitshop;

import android.app.Instrumentation;
import android.content.Context;
import android.content.ContextWrapper;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import androidx.test.core.app.ActivityScenario;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;
import com.example.fruitshop.activities.AdminOrdersActivity;
import com.example.fruitshop.core.BusinessClock;
import com.example.fruitshop.dal.AuthRepository;
import com.example.fruitshop.dal.DBHelper;
import com.example.fruitshop.dal.OrderRepository;
import com.example.fruitshop.dto.CartLine;
import com.example.fruitshop.models.Account;
import com.example.fruitshop.models.OrderStatus;
import com.example.fruitshop.session.SessionManager;
import java.io.File;
import java.io.FileOutputStream;
import java.util.Arrays;
import java.util.concurrent.TimeUnit;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.Espresso.pressBack;
import static androidx.test.espresso.action.ViewActions.*;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.*;
import static org.hamcrest.Matchers.containsString;
import static org.junit.Assert.assertEquals;

/** Real SQLite fixture with separate database/preferences; never writes the user's orders. */
@RunWith(AndroidJUnit4.class)
public class AdminOrdersUiInstrumentedTest {
    private static final String TEST_DB = "admin_order_ui_test.db";
    private Instrumentation instrumentation;
    private FruitShopApp app;
    private AuthRepository originalAuth;
    private OrderRepository originalOrders;
    private SessionManager originalSession;
    private DBHelper helper;
    private ActivityScenario<AdminOrdersActivity> scenario;
    private long admin, orderId;

    @Before public void setup() throws Exception {
        instrumentation = InstrumentationRegistry.getInstrumentation();
        app = (FruitShopApp) instrumentation.getTargetContext().getApplicationContext();
        originalAuth = app.auth; originalOrders = app.orders; originalSession = app.session;
        app.deleteDatabase(TEST_DB);
        BusinessClock clock = BusinessClock.fixed(1792717200000L, "Asia/Bangkok");
        helper = new DBHelper(app, TEST_DB, clock);
        app.auth = new AuthRepository(helper);
        app.orders = new OrderRepository(helper, app.auth, clock);
        app.session = new SessionManager(new ContextWrapper(app) {
            @Override public SharedPreferences getSharedPreferences(String name, int mode) {
                return super.getSharedPreferences("admin_order_ui_" + name, mode);
            }
        });
        Account account = app.databaseExecutor.submit(() -> {
            long customer = app.auth.login("customer01", "customer123").id;
            orderId = app.orders.place(customer, Arrays.asList(new CartLine(1, 500, 40000),
                    new CartLine(2, 2, 15000), new CartLine(3, 1, 25000)));
            return app.auth.login("admin01", "admin123");
        }).get(30, TimeUnit.SECONDS);
        admin = account.id;
        instrumentation.runOnMainSync(() -> app.session.signIn(account, false));
        scenario = ActivityScenario.launch(new Intent(app, AdminOrdersActivity.class)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK));
        awaitDatabase();
    }

    @After public void cleanup() throws Exception {
        // Finish every fixture activity, including a detail screen recreated by the test.
        instrumentation.runOnMainSync(() -> {
            androidx.test.runner.lifecycle.ActivityLifecycleMonitor monitor =
                    androidx.test.runner.lifecycle.ActivityLifecycleMonitorRegistry.getInstance();
            for (androidx.test.runner.lifecycle.Stage stage : androidx.test.runner.lifecycle.Stage.values()) {
                for (android.app.Activity activity : new java.util.ArrayList<>(monitor.getActivitiesInStage(stage))) {
                    activity.finish();
                }
            }
        });
        if (scenario != null) scenario.close();
        awaitDatabase();
        instrumentation.runOnMainSync(() -> {
            app.session.signOut();
            app.auth = originalAuth; app.orders = originalOrders; app.session = originalSession;
        });
        helper.close(); app.deleteDatabase(TEST_DB);
    }

    private void awaitDatabase() throws Exception {
        for (int i = 0; i < 4; i++) {
            app.databaseExecutor.submit(() -> {}).get(30, TimeUnit.SECONDS);
            instrumentation.waitForIdleSync();
        }
    }

    private void screenshot(String name) throws Exception {
        Bitmap bitmap = instrumentation.getUiAutomation().takeScreenshot();
        File file = new File(app.getExternalFilesDir(null), name + ".png");
        try (FileOutputStream output = new FileOutputStream(file)) {
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, output);
        } finally { bitmap.recycle(); }
    }

    @Test public void listAndDetailsShowCustomerAndPurchasedItems() throws Exception {
        onView(withId(R.id.admin_order_customer)).check(matches(withText("Nguyễn Minh An")));
        onView(withId(R.id.admin_order_contact)).check(matches(withText("0901234567")));
        onView(withId(R.id.admin_order_items_preview)).check(matches(withText(containsString("Cam sành"))));
        onView(withId(R.id.admin_order_status)).check(matches(withText(R.string.status_pending)));
        screenshot("admin-orders-list");
        onView(withId(R.id.admin_order_customer)).perform(click());
        awaitDatabase();
        onView(withId(R.id.admin_order_phone)).check(matches(withText(containsString("0901234567"))));
        onView(withId(R.id.admin_order_email)).check(matches(withText(containsString("minhan@example.com"))));
        onView(withText("Táo đỏ")).check(matches(isDisplayed()));
        onView(withId(R.id.admin_order_amount)).check(matches(withText(containsString("75.000"))));
        screenshot("admin-order-detail");
        // A recreated activity must reload the order and customer instead of relying on transient rows.
        instrumentation.runOnMainSync(() -> {
            android.app.Activity activity = androidx.test.runner.lifecycle.ActivityLifecycleMonitorRegistry.getInstance()
                    .getActivitiesInStage(androidx.test.runner.lifecycle.Stage.RESUMED).iterator().next();
            activity.recreate();
        });
        awaitDatabase();
        onView(withId(R.id.admin_order_email)).check(matches(withText(containsString("minhan@example.com"))));
    }

    @Test public void transitionsUpdateDetailsAndListWithoutSubtractingStockAgain() throws Exception {
        onView(withId(R.id.admin_order_customer)).perform(click()); awaitDatabase();
        onView(withId(R.id.admin_order_advance)).perform(click()); awaitDatabase();
        onView(withId(R.id.admin_order_status)).check(matches(withText(R.string.status_confirmed)));
        onView(withId(R.id.admin_order_advance)).check(matches(withText(R.string.admin_order_complete)));
        onView(withId(R.id.admin_order_advance)).perform(click()); awaitDatabase();
        onView(withId(R.id.admin_order_status)).check(matches(withText(R.string.status_completed)));
        onView(withId(R.id.admin_order_advance)).check(matches(withEffectiveVisibility(Visibility.GONE)));
        assertEquals(OrderStatus.COMPLETED, app.databaseExecutor.submit(() -> app.orders.detail(admin, orderId))
                .get(30, TimeUnit.SECONDS).order.status);
        long stock = app.databaseExecutor.submit(() -> {
            try (android.database.Cursor cursor = helper.getReadableDatabase().rawQuery("SELECT quantity FROM Fruit WHERE id=1", null)) {
                cursor.moveToFirst(); return cursor.getLong(0);
            }
        }).get(30, TimeUnit.SECONDS);
        assertEquals(11500, stock);
        pressBack(); awaitDatabase();
        onView(withId(R.id.admin_order_status)).check(matches(withText(R.string.status_completed)));
    }
}
