package com.example.studentmanager.session;

import android.content.Context;
import android.content.SharedPreferences;
import com.example.studentmanager.models.Account;

/** Non-remembered sessions live only in this Application instance, never on disk. */
public final class SessionManager {
    public static final String PREFERENCES = "login_session";
    private final SharedPreferences preferences;
    private long currentAccountId;

    public SessionManager(Context context) {
        preferences = context.getSharedPreferences(PREFERENCES, Context.MODE_PRIVATE);
        currentAccountId = preferences.getBoolean("remember", false) ? preferences.getLong("accountId", -1) : -1;
    }

    public synchronized long accountId() { return currentAccountId; }

    public synchronized void signIn(Account account, boolean remember) {
        currentAccountId = account.id;
        SharedPreferences.Editor editor = preferences.edit().clear();
        if (remember) editor.putBoolean("remember", true).putLong("accountId", account.id)
                .putString("username", account.username).putString("role", account.role);
        editor.apply();
    }

    public synchronized void signOut() {
        currentAccountId = -1;
        preferences.edit().clear().apply();
    }
}
