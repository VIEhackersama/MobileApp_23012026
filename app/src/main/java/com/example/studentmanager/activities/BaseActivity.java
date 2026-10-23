package com.example.studentmanager.activities;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.Toast;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import com.example.studentmanager.R;
import com.example.studentmanager.StudentManagerApp;
import com.example.studentmanager.models.Account;
import com.google.android.material.appbar.MaterialToolbar;
import java.util.concurrent.Callable;
import java.util.function.Consumer;

public abstract class BaseActivity extends AppCompatActivity {
    protected StudentManagerApp app;
    protected Account user;
    private int pendingTasks;

    @Override protected void onCreate(Bundle state) {
        super.onCreate(state);
        app = (StudentManagerApp) getApplication();
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

    protected void setupToolbar(String title, boolean back) {
        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        toolbar.setTitle(title);
        if (back) {
            toolbar.setNavigationIcon(R.drawable.ic_back);
            toolbar.setNavigationContentDescription(R.string.back);
            toolbar.setNavigationOnClickListener(v -> finish());
        }
        toolbar.getMenu().add(0, 1, 0, R.string.logout).setShowAsAction(android.view.MenuItem.SHOW_AS_ACTION_NEVER);
        toolbar.setOnMenuItemClickListener(item -> {
            if (item.getItemId() == 1) { signOut(); return true; }
            return false;
        });
    }

    protected boolean requiresAuthentication() { return true; }

    @Override protected void onResume() {
        super.onResume();
        if (requiresAuthentication()) {
            long id = app.session.accountId();
            if (id < 0) { signOut(); return; }
            submit(() -> app.repository.accountById(id), account -> {
                user = account;
                onAuthenticated();
            }, error -> signOut());
        }
    }

    protected void onAuthenticated() {}

    protected final <T> void submit(Callable<T> task, Consumer<T> success) {
        submit(task, success, error -> showError(message(error)));
    }

    protected final <T> void submit(Callable<T> task, Consumer<T> success, Consumer<Exception> failure) {
        pendingTasks++;
        setBusy(true);
        app.databaseExecutor.execute(() -> {
            try {
                T result = task.call();
                app.main.post(() -> {
                    if (isFinishing() || isDestroyed()) return;
                    pendingTasks--;
                    setBusy(pendingTasks > 0);
                    success.accept(result);
                });
            } catch (Exception error) {
                app.main.post(() -> {
                    if (isFinishing() || isDestroyed()) return;
                    pendingTasks--;
                    setBusy(pendingTasks > 0);
                    failure.accept(error);
                });
            }
        });
    }

    protected void setBusy(boolean busy) {
        ProgressBar progress = findViewById(R.id.progress);
        if (progress != null) progress.setVisibility(busy ? View.VISIBLE : View.GONE);
        View add = findViewById(R.id.add);
        if (add != null) add.setEnabled(!busy);
    }

    protected void showError(String error) { Toast.makeText(this, error, Toast.LENGTH_LONG).show(); }

    protected static String message(Exception error) {
        if (error instanceof IllegalArgumentException || error instanceof SecurityException) return error.getMessage();
        android.util.Log.e("StudentManager", "Local operation failed", error);
        return "Không thể hoàn tất thao tác. Vui lòng thử lại.";
    }

    protected static String text(View root, int id) {
        return ((EditText) root.findViewById(id)).getText().toString();
    }

    protected void saveDialog(AlertDialog dialog, View form, Callable<Void> save, Runnable refresh) {
        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setEnabled(false);
        dialog.getButton(AlertDialog.BUTTON_NEGATIVE).setEnabled(false);
        dialog.setCancelable(false);
        form.findViewById(R.id.form_error).setVisibility(View.GONE);
        submit(save, ignored -> {
            dialog.dismiss();
            Toast.makeText(this, "Đã lưu thay đổi.", Toast.LENGTH_SHORT).show();
            refresh.run();
        }, error -> {
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setEnabled(true);
            dialog.getButton(AlertDialog.BUTTON_NEGATIVE).setEnabled(true);
            dialog.setCancelable(true);
            android.widget.TextView label = form.findViewById(R.id.form_error);
            label.setText(message(error));
            label.setVisibility(View.VISIBLE);
        });
    }

    protected void signOut() {
        app.session.signOut();
        startActivity(new Intent(this, LoginActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK));
        finish();
    }
}
