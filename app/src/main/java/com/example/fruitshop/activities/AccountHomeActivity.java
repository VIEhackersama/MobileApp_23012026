package com.example.fruitshop.activities;

import android.os.Bundle;
import android.widget.TextView;
import com.example.fruitshop.R;

/** Shared account home layout for the Admin and Customer screens. */
public abstract class AccountHomeActivity extends BaseActivity {
    @Override protected void onCreate(Bundle state) {
        super.onCreate(state); setContentView(R.layout.activity_account_home); setupWindow();
        findViewById(R.id.logout).setOnClickListener(v -> signOut());
    }
    protected void label(int id, String value) { ((TextView) findViewById(id)).setText(value); }
}
