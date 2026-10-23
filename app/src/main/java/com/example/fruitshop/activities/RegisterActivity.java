package com.example.fruitshop.activities;

import android.os.Bundle;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.TextView;
import com.example.fruitshop.R;
import com.example.fruitshop.dto.RegistrationInput;
import com.google.android.material.appbar.MaterialToolbar;

public final class RegisterActivity extends BaseActivity {
    @Override protected boolean requiresAuthentication() { return false; }
    @Override protected void onCreate(Bundle state) {
        super.onCreate(state); setContentView(R.layout.activity_register); setupWindow();
        ((MaterialToolbar) findViewById(R.id.toolbar)).setNavigationOnClickListener(v -> finish());
        findViewById(R.id.register).setOnClickListener(v -> register());
        ((EditText) findViewById(R.id.confirm_password)).setOnEditorActionListener((v, action, event) -> {
            if (action == EditorInfo.IME_ACTION_DONE) {
                if (findViewById(R.id.register).isEnabled()) register();
                return true;
            }
            return false;
        });
    }
    private void register() {
        View root = findViewById(R.id.root);
        RegistrationInput input = new RegistrationInput(text(root, R.id.fullname), text(root, R.id.phone),
                text(root, R.id.email), text(root, R.id.username), text(root, R.id.password), text(root, R.id.confirm_password));
        findViewById(R.id.error).setVisibility(View.GONE);
        submit(() -> app.auth.registerCustomer(input), account -> {
            app.session.signIn(account, false);
            ((EditText) findViewById(R.id.password)).setText("");
            ((EditText) findViewById(R.id.confirm_password)).setText(""); openHome(account);
        });
    }
    @Override protected void setBusy(boolean busy) {
        super.setBusy(busy);
        for (int id : new int[]{R.id.fullname, R.id.phone, R.id.email, R.id.username, R.id.password, R.id.confirm_password, R.id.register}) {
            View view = findViewById(id); if (view != null) view.setEnabled(!busy);
        }
    }
    @Override protected void showError(String error) {
        TextView label = findViewById(R.id.error); label.setText(error); label.setVisibility(View.VISIBLE);
    }
}
