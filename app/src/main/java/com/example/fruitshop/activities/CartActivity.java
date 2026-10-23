package com.example.fruitshop.activities;

import android.content.Intent;
import android.os.Bundle;
import android.text.InputType;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AlertDialog;
import com.example.fruitshop.R;
import com.example.fruitshop.core.DomainException;
import com.example.fruitshop.core.Money;
import com.example.fruitshop.core.Quantities;
import com.example.fruitshop.dto.CartLine;
import com.example.fruitshop.models.OrderItem;
import com.example.fruitshop.models.Role;
import com.example.fruitshop.models.SaleUnit;
import com.google.android.material.appbar.MaterialToolbar;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.LongConsumer;

/** Owner: person 3. The signed-in customer's cart and checkout. */
public final class CartActivity extends BaseActivity {
    private ListView list;
    private List<OrderItem> rows = new ArrayList<>();
    private long total;

    @Override protected Role requiredRole() { return Role.CUSTOMER; }

    @Override protected void onCreate(Bundle state) {
        super.onCreate(state);
        setContentView(R.layout.activity_orders);
        setupWindow();
        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        toolbar.setTitle(R.string.cart_title);
        toolbar.setNavigationIcon(R.drawable.ic_back);
        toolbar.setNavigationContentDescription(R.string.back);
        toolbar.setNavigationOnClickListener(v -> finish());
        ((TextView) findViewById(R.id.orders_empty)).setText(R.string.cart_empty);
        list = findViewById(R.id.orders_list);
        list.setEmptyView(findViewById(R.id.orders_empty));
        list.setOnItemClickListener((parent, view, position, id) -> editLine(rows.get(position)));
        TextView checkout = findViewById(R.id.orders_action);
        checkout.setText(R.string.cart_checkout);
        checkout.setVisibility(View.VISIBLE);
        checkout.setOnClickListener(v -> confirmCheckout());
    }

    @Override protected void onAuthenticated() { reload(); }

    /** Reads current prices; a changed price updates the cart so the customer confirms the new total. */
    private void reload() {
        long actor = user.id, generation = app.session.generation();
        List<CartLine> lines = app.cart.lines(actor);
        submit(() -> app.orders.preview(actor, lines), items -> {
            if (generation != app.session.generation()) return;
            Set<Long> found = new HashSet<>();
            for (OrderItem item : items) found.add(item.fruitId);
            for (CartLine line : lines) {
                if (!found.contains(line.fruitId)) app.cart.remove(actor, line.fruitId); // fruit was deleted
            }
            for (OrderItem item : items) {
                for (CartLine line : lines) {
                    if (line.fruitId == item.fruitId && line.quotedUnitPrice != item.unitPrice) {
                        app.cart.remove(actor, line.fruitId);
                        app.cart.add(actor, new CartLine(line.fruitId, line.quantity, item.unitPrice));
                    }
                }
            }
            show(items);
        });
    }

    private void show(List<OrderItem> items) {
        rows = items;
        total = 0;
        List<String> text = new ArrayList<>();
        for (OrderItem item : items) {
            total = Money.add(total, item.lineTotal);
            text.add(getString(R.string.cart_row, item.fruitName, Quantities.format(item.quantity, item.unit),
                    Money.format(item.unitPrice), item.unit.label, Money.format(item.lineTotal)));
        }
        list.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, text));
        ((TextView) findViewById(R.id.orders_summary)).setText(getString(R.string.cart_total, Money.format(total)));
        findViewById(R.id.orders_action).setEnabled(!items.isEmpty());
    }

    private void editLine(OrderItem item) {
        long actor = user.id, fruitId = item.fruitId;
        askQuantity(this, item.fruitName, item.unit, quantity -> {
            app.cart.setQuantity(actor, fruitId, quantity);
            reload();
        }, () -> {
            app.cart.remove(actor, fruitId);
            reload();
        });
    }

    private void confirmCheckout() {
        new AlertDialog.Builder(this)
                .setTitle(R.string.cart_confirm_title)
                .setMessage(getString(R.string.cart_confirm_message, Money.format(total)))
                .setPositiveButton(R.string.cart_checkout, (dialog, which) -> placeOrder())
                .setNegativeButton(android.R.string.cancel, null)
                .show();
    }

    private void placeOrder() {
        long actor = user.id, generation = app.session.generation();
        List<CartLine> lines = app.cart.lines(actor);
        submit(() -> app.orders.place(actor, lines), orderId -> {
            if (generation != app.session.generation()) return;
            app.cart.clear(actor);
            Toast.makeText(this, getString(R.string.cart_placed, orderId), Toast.LENGTH_LONG).show();
            startActivity(new Intent(this, OrderListActivity.class));
            finish();
        }, error -> {
            if (generation != app.session.generation()) return;
            showError(message(error));
            reload(); // price/stock may have changed: show the latest cart before the customer tries again
        });
    }

    @Override protected void setBusy(boolean busy) {
        super.setBusy(busy);
        View checkout = findViewById(R.id.orders_action);
        if (checkout != null) checkout.setEnabled(!busy && !rows.isEmpty()); // no double orders
    }

    /** Quantity dialog: kg accepts up to 3 decimals, quả/phần only whole numbers. */
    private static void askQuantity(BaseActivity activity, String title, SaleUnit unit, LongConsumer onSave, Runnable onRemove) {
        EditText input = new EditText(activity);
        input.setInputType(unit == SaleUnit.KG
                ? InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL : InputType.TYPE_CLASS_NUMBER);
        input.setHint(activity.getString(R.string.cart_quantity_hint, unit.label));
        AlertDialog.Builder dialog = new AlertDialog.Builder(activity)
                .setTitle(title)
                .setView(input)
                .setPositiveButton(R.string.cart_save, (d, which) -> {
                    try {
                        long quantity = Quantities.parse(input.getText().toString(), unit);
                        if (quantity <= 0) activity.showError(activity.getString(R.string.cart_quantity_positive));
                        else onSave.accept(quantity);
                    } catch (DomainException error) { activity.showError(error.getMessage()); }
                })
                .setNegativeButton(android.R.string.cancel, null);
        dialog.setNeutralButton(R.string.cart_remove, (d, which) -> onRemove.run());
        dialog.show();
    }
}
