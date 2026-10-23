package com.example.fruitshop.activities;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.LinearLayout;
import com.example.fruitshop.R;
import com.example.fruitshop.models.Role;
import com.google.android.material.button.MaterialButton;

public final class AdminHomeActivity extends AccountHomeActivity {
    private MaterialButton ordersAction;
    @Override protected void onCreate(Bundle state) {
        super.onCreate(state);
        LinearLayout buttons = (LinearLayout) findViewById(R.id.logout).getParent();
        int logoutIndex = buttons.indexOfChild(findViewById(R.id.logout));
        MaterialButton catalog = actionButton(R.string.fruit_catalog);
        catalog.setOnClickListener(v -> startActivity(new Intent(this, FruitCatalogActivity.class)));
        buttons.addView(catalog, logoutIndex);
        ordersAction = actionButton(R.string.fruit_admin_orders);
        ordersAction.setOnClickListener(v -> startActivity(new Intent(this, AdminOrdersActivity.class)));
        buttons.addView(ordersAction, logoutIndex + 1);
    }

    @Override protected Role requiredRole() { return Role.ADMIN; }
    @Override protected void onAuthenticated() {
        // Person 1 wires an OrderDataSource provider when Person 3's repository is ready.
        ordersAction.setVisibility(getApplication() instanceof AdminOrdersActivity.OrderProvider ? View.VISIBLE : View.GONE);
    }

    private MaterialButton actionButton(int label) {
        MaterialButton button = new MaterialButton(this);
        button.setText(label);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(-1, (int) (56 * getResources().getDisplayMetrics().density));
        params.topMargin = (int) (12 * getResources().getDisplayMetrics().density);
        button.setLayoutParams(params);
        return button;
    }
}
