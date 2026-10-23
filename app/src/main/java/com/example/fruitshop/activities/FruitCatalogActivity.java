package com.example.fruitshop.activities;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.InputType;
import android.text.TextWatcher;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import androidx.appcompat.app.AlertDialog;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.example.fruitshop.R;
import com.example.fruitshop.contracts.FruitDataSource;
import com.example.fruitshop.core.BusinessDates;
import com.example.fruitshop.core.DomainException;
import com.example.fruitshop.core.ErrorCode;
import com.example.fruitshop.core.Money;
import com.example.fruitshop.core.Quantities;
import com.example.fruitshop.dal.FruitRepository;
import com.example.fruitshop.dto.FruitInput;
import com.example.fruitshop.models.Fruit;
import com.example.fruitshop.models.Role;
import com.example.fruitshop.models.SaleUnit;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import java.util.List;

/** Shared catalogue. Customer selection is returned as CartLine fields to the caller. */
public final class FruitCatalogActivity extends BaseActivity {
    public static final String EXTRA_FRUIT_ID = "fruit_id";
    public static final String EXTRA_QUANTITY = "quantity";
    public static final String EXTRA_QUOTED_PRICE = "quoted_price";

    private FruitDataSource fruits;
    private FruitCatalogAdapter adapter;
    private boolean admin;

    @Override protected void onCreate(Bundle state) {
        super.onCreate(state);
        setContentView(R.layout.activity_fruit_catalog);
        setupWindow();
        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        toolbar.setTitle(R.string.fruit_catalog);
        toolbar.setNavigationIcon(R.drawable.ic_back);
        toolbar.setNavigationContentDescription(R.string.back);
        toolbar.setNavigationOnClickListener(v -> finish());
        fruits = new FruitRepository(app.database, app.auth, app.clock);
        adapter = new FruitCatalogAdapter(app.clock, this::showDetail, this::edit, this::confirmDelete);
        RecyclerView list = findViewById(R.id.fruit_list);
        list.setLayoutManager(new LinearLayoutManager(this));
        list.setAdapter(adapter);
        ((EditText) findViewById(R.id.fruit_search)).addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) { if (user != null) reload(); }
            @Override public void afterTextChanged(Editable s) {}
        });
        findViewById(R.id.fruit_add).setOnClickListener(v -> edit(null));
    }

    @Override protected void onAuthenticated() {
        admin = user.role == Role.ADMIN;
        findViewById(R.id.fruit_add).setVisibility(admin ? View.VISIBLE : View.GONE);
        reload();
    }

    private void reload() {
        long actorId = user.id, generation = app.session.generation();
        String query = text(findViewById(R.id.root), R.id.fruit_search);
        submit(() -> fruits.list(actorId, query), result -> {
            if (generation != app.session.generation()) return;
            adapter.replace(result, admin);
            findViewById(R.id.fruit_empty).setVisibility(result.isEmpty() ? View.VISIBLE : View.GONE);
            ((TextView) findViewById(R.id.fruit_count)).setText(getString(R.string.fruit_count, result.size()));
        });
    }

    private void showDetail(Fruit row) {
        long actorId = user.id;
        submit(() -> fruits.detail(actorId, row.id), fruit -> {
            String state = BusinessDates.isExpired(fruit.expiryDate, app.clock) ? getString(R.string.fruit_expired)
                    : fruit.quantity == 0 ? getString(R.string.fruit_out_of_stock) : getString(R.string.fruit_available);
            String details = fruit.category + " · " + state + "\n"
                    + getString(R.string.fruit_price_detail, Money.format(fruit.price), fruit.unit.label) + "\n"
                    + getString(R.string.fruit_stock_detail, Quantities.format(fruit.quantity, fruit.unit)) + "\n"
                    + getString(R.string.fruit_expiry_detail, fruit.expiryDate) + "\n\n" + fruit.description;
            MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(this)
                    .setTitle(fruit.name).setMessage(details).setNegativeButton(R.string.fruit_close, null);
            if (!admin) builder.setPositiveButton(R.string.fruit_choose, (dialog, which) -> chooseQuantity(fruit));
            builder.show();
        });
    }

    private void chooseQuantity(Fruit fruit) {
        EditText amount = new EditText(this);
        amount.setHint(fruit.unit == SaleUnit.KG ? R.string.fruit_amount_kg : R.string.fruit_amount_count);
        amount.setInputType(fruit.unit == SaleUnit.KG ? InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL
                : InputType.TYPE_CLASS_NUMBER);
        int padding = (int) (24 * getResources().getDisplayMetrics().density);
        amount.setPadding(padding, padding / 2, padding, padding / 2);
        AlertDialog dialog = new MaterialAlertDialogBuilder(this)
                .setTitle(getString(R.string.fruit_choose_amount, fruit.name)).setView(amount)
                .setNegativeButton(R.string.fruit_cancel, null).setPositiveButton(R.string.fruit_choose, null).create();
        dialog.setOnShowListener(ignored -> dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
            long quantity;
            try {
                quantity = Quantities.parse(amount.getText().toString(), fruit.unit);
                if (quantity <= 0) throw new DomainException(ErrorCode.INVALID_INPUT, getString(R.string.fruit_positive_amount));
            } catch (DomainException error) { amount.setError(error.getMessage()); return; }
            long actorId = user.id;
            submit(() -> fruits.detail(actorId, fruit.id), current -> {
                if (current.price != fruit.price) {
                    amount.setError(getString(R.string.fruit_price_changed));
                    return;
                }
                if (quantity > current.quantity) { amount.setError(getString(R.string.fruit_quantity_exceeds)); return; }
                Intent result = new Intent().putExtra(EXTRA_FRUIT_ID, current.id)
                        .putExtra(EXTRA_QUANTITY, quantity).putExtra(EXTRA_QUOTED_PRICE, current.price);
                setResult(RESULT_OK, result);
                dialog.dismiss();
                finish();
            }, error -> amount.setError(message(error)));
        }));
        dialog.show();
    }

    private void edit(Fruit fruit) {
        if (!admin) return;
        View form = getLayoutInflater().inflate(R.layout.form_fruit, null);
        Spinner unit = form.findViewById(R.id.fruit_unit);
        SaleUnit[] units = SaleUnit.values();
        String[] labels = new String[units.length];
        for (int i = 0; i < units.length; i++) labels[i] = units[i].name() + " (" + units[i].label + ")";
        unit.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, labels));
        if (fruit != null) {
            put(form, R.id.fruit_name, fruit.name);
            put(form, R.id.fruit_category, fruit.category);
            put(form, R.id.fruit_price, Long.toString(fruit.price));
            put(form, R.id.fruit_quantity, Quantities.format(fruit.quantity, fruit.unit).split(" ")[0]);
            put(form, R.id.fruit_expiry, fruit.expiryDate);
            put(form, R.id.fruit_description, fruit.description);
            unit.setSelection(fruit.unit.ordinal());
            unit.setEnabled(false);
        }
        AlertDialog dialog = new MaterialAlertDialogBuilder(this)
                .setTitle(fruit == null ? R.string.fruit_add_title : R.string.fruit_edit_title)
                .setView(form).setNegativeButton(R.string.fruit_cancel, null)
                .setPositiveButton(R.string.fruit_save, null).create();
        dialog.setOnShowListener(ignored -> dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
            long actorId = user.id;
            String name = text(form, R.id.fruit_name), category = text(form, R.id.fruit_category);
            String price = text(form, R.id.fruit_price), quantity = text(form, R.id.fruit_quantity);
            String expiry = text(form, R.id.fruit_expiry), description = text(form, R.id.fruit_description);
            SaleUnit selectedUnit = units[unit.getSelectedItemPosition()];
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setEnabled(false);
            submit(() -> {
                long parsedPrice;
                try { parsedPrice = Long.parseLong(price.trim()); }
                catch (NumberFormatException error) {
                    throw new DomainException(ErrorCode.INVALID_INPUT, getString(R.string.fruit_price_integer), error);
                }
                FruitInput input = new FruitInput(name, category, parsedPrice,
                        Quantities.parse(quantity, selectedUnit), expiry, description, selectedUnit);
                if (fruit == null) fruits.create(actorId, input);
                else fruits.update(actorId, fruit.id, input);
                return null;
            }, ignoredResult -> { dialog.dismiss(); reload(); }, error -> {
                dialog.getButton(AlertDialog.BUTTON_POSITIVE).setEnabled(true);
                TextView label = form.findViewById(R.id.form_error);
                label.setText(message(error)); label.setVisibility(View.VISIBLE);
            });
        }));
        dialog.show();
    }

    private void confirmDelete(Fruit fruit) {
        if (!admin) return;
        new MaterialAlertDialogBuilder(this).setTitle(getString(R.string.fruit_delete_title, fruit.name))
                .setMessage(R.string.fruit_delete_message).setNegativeButton(R.string.fruit_cancel, null)
                .setPositiveButton(R.string.delete, (dialog, which) -> submit(() -> {
                    fruits.delete(user.id, fruit.id); return null;
                }, ignored -> reload())).show();
    }

    private static void put(View root, int id, String value) { ((EditText) root.findViewById(id)).setText(value); }
}
