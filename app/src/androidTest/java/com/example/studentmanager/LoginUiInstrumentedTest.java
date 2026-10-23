package com.example.studentmanager;

import android.app.Instrumentation;
import androidx.test.core.app.ActivityScenario;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;
import com.example.studentmanager.activities.LoginActivity;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import java.util.concurrent.TimeUnit;
import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.*;
import static androidx.test.espresso.assertion.ViewAssertions.*;
import static androidx.test.espresso.matcher.ViewMatchers.*;
import static org.hamcrest.Matchers.*;

@RunWith(AndroidJUnit4.class)
public class LoginUiInstrumentedTest {
    private ActivityScenario<LoginActivity> scenario;
    private StudentManagerApp app;

    @Before public void setup() {
        app = (StudentManagerApp) InstrumentationRegistry.getInstrumentation().getTargetContext().getApplicationContext();
        app.session.signOut();
        scenario = ActivityScenario.launch(LoginActivity.class);
    }

    @After public void cleanup() {
        scenario.close();
        app.session.signOut();
    }

    private void awaitDatabase() throws Exception {
        Instrumentation instrumentation = InstrumentationRegistry.getInstrumentation();
        // Drain background work and its main-thread callbacks, including follow-up list loads.
        for (int i = 0; i < 4; i++) {
            app.databaseExecutor.submit(() -> {}).get(30, TimeUnit.SECONDS);
            instrumentation.waitForIdleSync();
        }
    }

    private void login(String username, String password) throws Exception {
        onView(withId(R.id.username)).perform(replaceText(username));
        onView(withId(R.id.password)).perform(replaceText(password), closeSoftKeyboard());
        onView(withId(R.id.login)).perform(scrollTo(), click());
        awaitDatabase();
    }

    @Test public void badCredentialsStayOnLoginAndShowError() throws Exception {
        login("teacher01", "incorrect");
        onView(withId(R.id.error)).check(matches(allOf(isDisplayed(), withText(containsString("không đúng")))));
        onView(withId(R.id.login)).check(matches(isEnabled()));
    }

    @Test public void teacherCanOpenStudentListAndFilter() throws Exception {
        login("teacher01", "teacher123");
        onView(withId(R.id.role)).check(matches(withText("KHÔNG GIAN GIÁO VIÊN")));
        onView(withId(R.id.students)).perform(scrollTo(), click());
        awaitDatabase();
        onView(withId(R.id.add)).check(matches(isDisplayed()));
        onView(withId(R.id.search)).perform(replaceText("SV001"), closeSoftKeyboard());
        onView(withText("Lê Thu Hà")).check(matches(isDisplayed()));
        onView(withText("Phạm Đức Huy")).check(doesNotExist());
    }

    @Test public void studentSeesOnlyOwnScoresWithoutEditControls() throws Exception {
        login("student01", "student123");
        onView(withId(R.id.role)).check(matches(withText("KHÔNG GIAN SINH VIÊN")));
        onView(withId(R.id.students)).check(matches(withEffectiveVisibility(Visibility.GONE)));
        onView(withId(R.id.scores)).perform(scrollTo(), click());
        awaitDatabase();
        onView(withId(R.id.add)).check(matches(withEffectiveVisibility(Visibility.GONE)));
        onView(withId(R.id.search)).perform(replaceText("SV002"), closeSoftKeyboard());
        onView(withId(R.id.empty)).check(matches(isDisplayed()));
        onView(withId(R.id.search)).perform(replaceText("SV001"), closeSoftKeyboard());
        onView(withId(R.id.empty)).check(matches(withEffectiveVisibility(Visibility.GONE)));
        onView(withId(R.id.list)).check(matches(hasDescendant(withText(containsString("SV001")))));
        onView(withId(R.id.list)).check(matches(not(hasDescendant(allOf(withId(R.id.item_edit), isDisplayed())))));
    }
}
