package com.example.fruitshop.activities;

import android.content.Intent;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.widget.Toast;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import com.example.fruitshop.R;
import com.example.fruitshop.core.DomainException;
import com.example.fruitshop.dto.CartLine;
import com.example.fruitshop.models.Role;
import com.google.android.material.appbar.MaterialToolbar;

/** Person 3 may extend this account home when implementing the Customer module. */
public final class CustomerHomeActivity extends AccountHomeActivity {
    /** Person 2's catalog returns the chosen fruit, quantity and the price the customer saw. */
    private final ActivityResultLauncher<Intent> pickFruit = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(), result -> {
                Intent data = result.getData();
                if (result.getResultCode() != RESULT_OK || data == null) return;
                try {
                    app.cart.add(app.session.accountId(), new CartLine(
                            data.getLongExtra(FruitCatalogActivity.EXTRA_FRUIT_ID, -1),
                            data.getLongExtra(FruitCatalogActivity.EXTRA_QUANTITY, 0),
                            data.getLongExtra(FruitCatalogActivity.EXTRA_QUOTED_PRICE, 0)));
                    Toast.makeText(this, R.string.cart_added, Toast.LENGTH_SHORT).show();
                } catch (DomainException error) { showError(error.getMessage()); }
            });

    @Override protected Role requiredRole() { return Role.CUSTOMER; }
    @Override protected void onCreate(Bundle state) {
        super.onCreate(state);
        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        toolbar.getMenu().add(R.string.catalog_title).setShowAsActionFlags(MenuItem.SHOW_AS_ACTION_IF_ROOM)
                .setOnMenuItemClickListener(item -> { pickFruit.launch(new Intent(this, FruitCatalogActivity.class)); return true; });
        toolbar.getMenu().add(R.string.cart_title).setShowAsActionFlags(MenuItem.SHOW_AS_ACTION_IF_ROOM)
                .setOnMenuItemClickListener(item -> { startActivity(new Intent(this, CartActivity.class)); return true; });
        toolbar.getMenu().add(R.string.orders_title).setShowAsActionFlags(MenuItem.SHOW_AS_ACTION_IF_ROOM)
                .setOnMenuItemClickListener(item -> { startActivity(new Intent(this, OrderListActivity.class)); return true; });
    }
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
