package com.example.fruitshop.activities;

import com.example.fruitshop.R;
import com.example.fruitshop.models.Role;

/** Person 2 may extend this account home when implementing the Admin module. */
public final class AdminHomeActivity extends AccountHomeActivity {
    @Override protected Role requiredRole() { return Role.ADMIN; }
    @Override protected void onAuthenticated() {
        accountLabels(R.string.admin_label, R.string.admin_description);
        label(R.id.greeting, getString(R.string.greeting, user.username));
    }
}
