package com.example.fruitshop.activities;

import android.view.View;
import com.example.fruitshop.R;
import com.example.fruitshop.models.Role;

/** Person 3 may extend this account home when implementing the Customer module. */
public final class CustomerHomeActivity extends AccountHomeActivity {
    @Override protected Role requiredRole() { return Role.CUSTOMER; }
    @Override protected void onAuthenticated() {
        accountLabels(R.string.customer_label, R.string.customer_description);
        long actorId = user.id, generation = app.session.generation();
        submit(() -> app.auth.requireCustomer(actorId), profile -> {
            if (generation != app.session.generation()) return;
            label(R.id.greeting, getString(R.string.greeting, profile.fullname));
            label(R.id.profile, getString(R.string.customer_profile, profile.fullname, profile.phone, profile.email));
            findViewById(R.id.profile).setVisibility(View.VISIBLE);
        });
    }
}
