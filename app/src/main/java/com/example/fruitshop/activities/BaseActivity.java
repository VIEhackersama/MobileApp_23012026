package com.example.fruitshop.activities;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import com.example.fruitshop.FruitShopApp;
import com.example.fruitshop.R;
import com.example.fruitshop.core.DomainException;
import com.example.fruitshop.core.ErrorCode;
import com.example.fruitshop.models.Account;
import com.example.fruitshop.models.Role;
import java.util.concurrent.Callable;
import java.util.function.Consumer;

public abstract class BaseActivity extends AppCompatActivity {
    protected FruitShopApp app;
    protected Account user;
    private int pending;
    @Override protected void onCreate(Bundle state) {
        super.onCreate(state); app = (FruitShopApp) getApplication();
    }
    protected void setupWindow() {
        View root = findViewById(R.id.root);
        ViewCompat.setOnApplyWindowInsetsListener(root, (view, insets) -> {
            Insets bars = insets.getInsets(WindowInsetsCompat.Type.systemBars() | WindowInsetsCompat.Type.displayCutout());
            Insets ime = insets.getInsets(WindowInsetsCompat.Type.ime());
            view.setPadding(bars.left, bars.top, bars.right, Math.max(bars.bottom, ime.bottom));
            return insets;
        });
        ViewCompat.requestApplyInsets(root);
    }
    protected boolean requiresAuthentication() { return true; }
    protected Role requiredRole() { return null; }
    @Override protected void onResume() {
        super.onResume();
        if (!requiresAuthentication()) return;
        long id = app.session.accountId(), generation = app.session.generation();
        if (id < 0) { signOut(); return; }
        submit(() -> app.auth.accountById(id), account -> {
            if (generation != app.session.generation()) return;
            if (requiredRole() != null && requiredRole() != account.role) { openHome(account); return; }
            user = account; onAuthenticated();
        }, error -> {
            if (generation != app.session.generation()) return;
            if (error instanceof DomainException && ((DomainException) error).code == ErrorCode.SESSION_EXPIRED) signOut();
            else showError(message(error));
        });
    }
    protected void onAuthenticated() {}
    protected final <T> void submit(Callable<T> task, Consumer<T> success) {
        submit(task, success, error -> showError(message(error)));
    }
    protected final <T> void submit(Callable<T> task, Consumer<T> success, Consumer<Exception> failure) {
        pending++; setBusy(true);
        app.databaseExecutor.execute(() -> {
            try {
                T value = task.call();
                app.main.post(() -> {
                    if (isDestroyed() || isFinishing()) return;
                    pending--; setBusy(pending > 0); success.accept(value);
                });
            } catch (Exception error) {
                app.main.post(() -> {
                    if (isDestroyed() || isFinishing()) return;
                    pending--; setBusy(pending > 0); failure.accept(error);
                });
            }
        });
    }
    protected void setBusy(boolean busy) {
        ProgressBar progress = findViewById(R.id.progress);
        if (progress != null) progress.setVisibility(busy ? View.VISIBLE : View.GONE);
    }
    protected void showError(String error) { Toast.makeText(this, error, Toast.LENGTH_LONG).show(); }
    protected String message(Exception error) {
        if (error instanceof DomainException) return error.getMessage();
        android.util.Log.e("FruitShop", "Local operation failed", error);
        return getString(R.string.unexpected_error);
    }
    protected static String text(View root, int id) { return ((EditText) root.findViewById(id)).getText().toString(); }
    protected void openHome(Account account) {
        Class<?> destination = account.role == Role.ADMIN ? AdminHomeActivity.class : CustomerHomeActivity.class;
        startActivity(new Intent(this, destination).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK));
        finish();
    }
    protected void signOut() {
        app.session.signOut();
        startActivity(new Intent(this, LoginActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK));
        finish();
    }
}
