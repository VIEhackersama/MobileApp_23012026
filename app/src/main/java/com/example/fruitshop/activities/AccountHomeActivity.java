package com.example.fruitshop.activities;

import android.os.Bundle;
import android.widget.TextView;
import com.example.fruitshop.R;

/** Account-only foundation UI. Business modules are deliberately not wired yet. */
public abstract class AccountHomeActivity extends BaseActivity {
    @Override protected void onCreate(Bundle state) {
        super.onCreate(state); setContentView(R.layout.activity_account_home); setupWindow();
        findViewById(R.id.logout).setOnClickListener(v -> signOut());
    }
    protected void accountLabels(int roleLabel, int description) {
        label(R.id.role, getString(roleLabel)); label(R.id.description, getString(description));
        label(R.id.account_username, getString(R.string.account_username, user.username));
        label(R.id.account_role, getString(R.string.account_role, user.role.name()));
    }
    protected void label(int id, String value) { ((TextView) findViewById(id)).setText(value); }
}
