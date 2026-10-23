package com.example.fruitshop.session;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;
import com.example.fruitshop.models.Account;
import java.util.concurrent.CopyOnWriteArrayList;

public final class SessionManager {
    public static final String PREFERENCES = "fruit_shop_session";
    public interface Listener { void onSessionChanged(long previousAccountId, long currentAccountId); }
    private final SharedPreferences preferences;
    private final CopyOnWriteArrayList<Listener> listeners = new CopyOnWriteArrayList<>();
    private long currentAccountId;
    private long generation;
    public SessionManager(Context context) {
        preferences = context.getSharedPreferences(PREFERENCES, Context.MODE_PRIVATE);
        currentAccountId = preferences.getBoolean("remember", false) ? preferences.getLong("accountId", -1) : -1;
    }
    public synchronized long accountId() { return currentAccountId; }
    public synchronized long generation() { return generation; }
    public void addListener(Listener listener) { listeners.addIfAbsent(listener); }
    public void removeListener(Listener listener) { listeners.remove(listener); }
    /** Called by activities on the main thread after database authentication succeeds. */
    public void signIn(Account account, boolean remember) {
        long previous;
        synchronized (this) {
            previous = currentAccountId; currentAccountId = account.id; generation++;
            SharedPreferences.Editor editor = preferences.edit().clear();
            if (remember) editor.putBoolean("remember", true).putLong("accountId", account.id)
                    .putString("username", account.username).putString("role", account.role.name());
            editor.apply();
        }
        notifyListeners(previous, account.id);
    }
    public void signOut() {
        long previous;
        synchronized (this) {
            previous = currentAccountId; currentAccountId = -1; generation++;
            preferences.edit().clear().apply();
        }
        notifyListeners(previous, -1);
    }
    private void notifyListeners(long previous, long current) {
        for (Listener listener : listeners) {
            try { listener.onSessionChanged(previous, current); }
            catch (RuntimeException error) { Log.e("FruitShop", "Session listener failed", error); }
        }
    }
}
