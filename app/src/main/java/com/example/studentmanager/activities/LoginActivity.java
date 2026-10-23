package com.example.studentmanager.activities;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.TextView;
import com.example.studentmanager.MainActivity;
import com.example.studentmanager.R;

public final class LoginActivity extends BaseActivity {
    @Override protected boolean requiresAuthentication() { return false; }

    @Override protected void onCreate(Bundle state) {
        super.onCreate(state);
        setContentView(R.layout.activity_login);
        setupWindow();
        findViewById(R.id.login).setOnClickListener(v -> login());
        ((EditText) findViewById(R.id.password)).setOnEditorActionListener((v, action, event) -> {
            if (action == EditorInfo.IME_ACTION_DONE) { if (findViewById(R.id.login).isEnabled()) login(); return true; }
            return false;
        });
        if (app.session.accountId() > 0) {
            long id = app.session.accountId();
            submit(() -> app.repository.accountById(id), account -> openHome(), error -> app.session.signOut());
        }
    }

    private void login() {
        String username = text(findViewById(R.id.root), R.id.username);
        String password = text(findViewById(R.id.root), R.id.password);
        boolean remember = ((CheckBox) findViewById(R.id.remember)).isChecked();
        findViewById(R.id.error).setVisibility(View.GONE);
        submit(() -> app.repository.login(username, password), account -> {
            app.session.signIn(account, remember);
            ((EditText) findViewById(R.id.password)).setText("");
            openHome();
        });
    }

    private void openHome() {
        startActivity(new Intent(this, MainActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK));
        finish();
    }

    @Override protected void setBusy(boolean busy) {
        super.setBusy(busy);
        findViewById(R.id.login).setEnabled(!busy);
        findViewById(R.id.username).setEnabled(!busy);
        findViewById(R.id.password).setEnabled(!busy);
        findViewById(R.id.remember).setEnabled(!busy);
    }

    @Override protected void showError(String error) {
        TextView label = findViewById(R.id.error);
        label.setText(error);
        label.setVisibility(View.VISIBLE);
    }
}
