package com.example.fruitshop.activities;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import com.example.fruitshop.R;
import com.example.fruitshop.core.DomainException;
import com.example.fruitshop.core.ErrorCode;
import com.example.fruitshop.core.Money;
import com.example.fruitshop.core.Quantities;
import com.example.fruitshop.dal.FruitRepository;
import com.example.fruitshop.models.Fruit;
import com.example.fruitshop.models.Role;
import com.google.android.material.appbar.MaterialToolbar;

public final class CustomerHomeActivity extends AccountHomeActivity {
    private static final String STATE_ACCOUNT_ID = "selected_account_id";
    private static final String STATE_FRUIT_ID = "selected_fruit_id";
    private static final String STATE_QUANTITY = "selected_quantity";
    private static final String STATE_PRICE = "selected_price";

    private ActivityResultLauncher<Intent> catalogLauncher;
    private long selectedAccountId = -1, selectedFruitId = -1, selectedQuantity, selectedPrice;

    @Override protected void onCreate(Bundle state) {
        super.onCreate(state);
        if (state != null) {
            selectedAccountId = state.getLong(STATE_ACCOUNT_ID, -1);
            selectedFruitId = state.getLong(STATE_FRUIT_ID, -1);
            selectedQuantity = state.getLong(STATE_QUANTITY, 0);
            selectedPrice = state.getLong(STATE_PRICE, 0);
        }
        catalogLauncher = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
            Intent data = result.getData();
            if (result.getResultCode() != Activity.RESULT_OK || data == null) return;
            if (!data.hasExtra(FruitCatalogActivity.EXTRA_FRUIT_ID)
                    || !data.hasExtra(FruitCatalogActivity.EXTRA_QUANTITY)
                    || !data.hasExtra(FruitCatalogActivity.EXTRA_QUOTED_PRICE)) return;
            long fruitId = data.getLongExtra(FruitCatalogActivity.EXTRA_FRUIT_ID, -1);
            long quantity = data.getLongExtra(FruitCatalogActivity.EXTRA_QUANTITY, 0);
            long price = data.getLongExtra(FruitCatalogActivity.EXTRA_QUOTED_PRICE, 0);
            if (fruitId <= 0 || quantity <= 0 || price <= 0) return;
            selectedAccountId = app.session.accountId();
            selectedFruitId = fruitId;
            selectedQuantity = quantity;
            selectedPrice = price;
            showSelection();
        });
        findViewById(R.id.customer_catalog).setOnClickListener(v ->
                catalogLauncher.launch(new Intent(this, FruitCatalogActivity.class)));
    }

    @Override protected Role requiredRole() { return Role.CUSTOMER; }

    @Override protected void onAuthenticated() {
        findViewById(R.id.customer_catalog).setVisibility(View.VISIBLE);
        long actorId = user.id;
        if (selectedFruitId > 0) {
            if (selectedAccountId == actorId) showSelection();
            else clearSelection();
        }
    }

    private void showSelection() {
        long actorId = app.session.accountId(), generation = app.session.generation();
        long fruitId = selectedFruitId, quantity = selectedQuantity, price = selectedPrice;
        if (actorId <= 0 || actorId != selectedAccountId || fruitId <= 0) return;
        submit(() -> {
            Fruit fruit = new FruitRepository(app.database, app.auth, app.clock).detail(actorId, fruitId);
            if (fruit.price != price) throw new DomainException(ErrorCode.PRICE_CHANGED,
                    getString(R.string.fruit_selection_price_changed));
            return new SelectionPreview(fruit, Money.lineTotal(price, quantity, fruit.unit));
        }, preview -> {
            if (generation != app.session.generation() || fruitId != selectedFruitId
                    || quantity != selectedQuantity || price != selectedPrice) return;
            label(R.id.customer_selection_details, getString(R.string.fruit_selection_details,
                    preview.fruit.name, Quantities.format(quantity, preview.fruit.unit), Money.format(preview.total)));
            findViewById(R.id.customer_selection).setVisibility(View.VISIBLE);
        }, error -> {
            if (generation != app.session.generation() || fruitId != selectedFruitId
                    || quantity != selectedQuantity || price != selectedPrice) return;
            clearSelection();
            showError(message(error));
        });
    }

    private void clearSelection() {
        selectedAccountId = -1;
        selectedFruitId = -1;
        selectedQuantity = 0;
        selectedPrice = 0;
        findViewById(R.id.customer_selection).setVisibility(View.GONE);
    }

    @Override protected void onSaveInstanceState(Bundle state) {
        state.putLong(STATE_ACCOUNT_ID, selectedAccountId);
        state.putLong(STATE_FRUIT_ID, selectedFruitId);
        state.putLong(STATE_QUANTITY, selectedQuantity);
        state.putLong(STATE_PRICE, selectedPrice);
        super.onSaveInstanceState(state);
    }

    private static final class SelectionPreview {
        final Fruit fruit;
        final long total;
        SelectionPreview(Fruit fruit, long total) { this.fruit = fruit; this.total = total; }
    }
}
