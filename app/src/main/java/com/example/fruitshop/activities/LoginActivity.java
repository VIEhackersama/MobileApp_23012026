package com.example.fruitshop.activities;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.TextView;
import com.example.fruitshop.R;
import com.example.fruitshop.core.DomainException;
import com.example.fruitshop.core.ErrorCode;

public final class LoginActivity extends BaseActivity {
    @Override protected boolean requiresAuthentication() { return false; }
    @Override protected void onCreate(Bundle state) {
        super.onCreate(state); setContentView(R.layout.activity_login); setupWindow();
        findViewById(R.id.login).setOnClickListener(v -> login());
        findViewById(R.id.open_register).setOnClickListener(v -> startActivity(new Intent(this, RegisterActivity.class)));
        ((EditText) findViewById(R.id.password)).setOnEditorActionListener((v, action, event) -> {
            if (action == EditorInfo.IME_ACTION_DONE) {
                if (findViewById(R.id.login).isEnabled()) login();
                return true;
            }
            return false;
        });
    }
    @Override protected void onResume() {
        super.onResume();
        long accountId = app.session.accountId(), generation = app.session.generation();
        if (accountId > 0) submit(() -> app.auth.accountById(accountId), account -> {
            if (generation == app.session.generation()) openHome(account);
        }, error -> {
            if (generation != app.session.generation()) return;
            if (error instanceof DomainException && ((DomainException) error).code == ErrorCode.SESSION_EXPIRED) app.session.signOut();
            else showError(message(error));
        });
    }
    private void login() {
        View root = findViewById(R.id.root);
        String username = text(root, R.id.username), password = text(root, R.id.password);
        boolean remember = ((CheckBox) findViewById(R.id.remember)).isChecked();
        findViewById(R.id.error).setVisibility(View.GONE);
        submit(() -> app.auth.login(username, password), account -> {
            app.session.signIn(account, remember);
            ((EditText) findViewById(R.id.password)).setText(""); openHome(account);
        });
    }
    @Override protected void setBusy(boolean busy) {
        super.setBusy(busy);
        for (int id : new int[]{R.id.login, R.id.open_register, R.id.username, R.id.password, R.id.remember}) {
            View view = findViewById(id); if (view != null) view.setEnabled(!busy);
        }
    }
    @Override protected void showError(String error) {
        TextView label = findViewById(R.id.error); label.setText(error); label.setVisibility(View.VISIBLE);
    }
}
