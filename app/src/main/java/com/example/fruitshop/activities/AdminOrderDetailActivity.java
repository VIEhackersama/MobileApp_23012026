package com.example.fruitshop.activities;

import android.os.Bundle;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import com.example.fruitshop.R;
import com.example.fruitshop.contracts.OrderDataSource;
import com.example.fruitshop.core.Money;
import com.example.fruitshop.core.Quantities;
import com.example.fruitshop.dto.OrderDetails;
import com.example.fruitshop.models.OrderItem;
import com.example.fruitshop.models.OrderStatus;
import com.example.fruitshop.models.Role;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;

/** Full order details with customer contact and immutable purchased-item snapshots. */
public final class AdminOrderDetailActivity extends BaseActivity {
    public static final String EXTRA_ORDER_ID = "order_id";
    private OrderDataSource orders;
    private OrderDetails details;
    private long orderId;
    private boolean busy;

    @Override protected Role requiredRole() { return Role.ADMIN; }

    @Override protected void onCreate(Bundle state) {
        super.onCreate(state);
        setContentView(R.layout.activity_admin_order_detail);
        setupWindow();
        orderId = getIntent().getLongExtra(EXTRA_ORDER_ID, -1);
        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        toolbar.setTitle(getString(R.string.admin_order_number, orderId));
        toolbar.setNavigationIcon(R.drawable.ic_back);
        toolbar.setNavigationContentDescription(R.string.back);
        toolbar.setNavigationOnClickListener(v -> finish());
        findViewById(R.id.admin_order_retry).setOnClickListener(v -> { if (user != null) reload(); });
        findViewById(R.id.admin_order_advance).setOnClickListener(v -> advance());
    }

    @Override protected void onAuthenticated() {
        if (!(getApplication() instanceof AdminOrdersActivity.OrderProvider)) {
            showError(getString(R.string.fruit_orders_unavailable)); finish(); return;
        }
        orders = ((AdminOrdersActivity.OrderProvider) getApplication()).orderDataSource();
        reload();
    }

    private void reload() {
        long actor = user.id, generation = app.session.generation();
        findViewById(R.id.admin_order_error).setVisibility(View.GONE);
        submit(() -> orders.detail(actor, orderId), result -> {
            if (generation != app.session.generation()) return;
            bind(result);
        }, error -> {
            if (generation != app.session.generation()) return;
            // Do not leave a stale transition button active when refreshing fails.
            details = null;
            findViewById(R.id.admin_order_footer).setVisibility(View.GONE);
            ((TextView) findViewById(R.id.admin_order_error_message)).setText(
                    getString(R.string.admin_orders_load_error, message(error)));
            findViewById(R.id.admin_order_error).setVisibility(View.VISIBLE);
        });
    }

    private void bind(OrderDetails result) {
        details = result;
        OrderUi.status(findViewById(R.id.admin_order_status), result.order.status);
        text(R.id.admin_order_date, OrderUi.date(result.order.orderDate));
        text(R.id.admin_order_customer, result.customer == null
                ? getString(R.string.admin_order_customer_unknown) : result.customer.fullname);
        text(R.id.admin_order_phone, result.customer == null ? "" : getString(R.string.admin_order_phone, result.customer.phone));
        text(R.id.admin_order_email, result.customer == null ? "" : getString(R.string.admin_order_email, result.customer.email));
        text(R.id.admin_order_item_count, getString(R.string.admin_order_item_count, result.items.size()));
        LinearLayout items = findViewById(R.id.admin_order_items);
        items.removeAllViews();
        for (OrderItem item : result.items) {
            View row = getLayoutInflater().inflate(R.layout.item_admin_order_product, items, false);
            ((TextView) row.findViewById(R.id.admin_order_product_name)).setText(item.fruitName);
            ((TextView) row.findViewById(R.id.admin_order_product_quantity)).setText(
                    getString(R.string.admin_order_item_price, Quantities.format(item.quantity, item.unit),
                            Money.format(item.unitPrice), item.unit.label));
            ((TextView) row.findViewById(R.id.admin_order_product_amount)).setText(Money.format(item.lineTotal));
            items.addView(row);
        }
        text(R.id.admin_order_amount, Money.format(result.order.totalAmount));
        OrderStatus next = OrderUi.next(result.order.status);
        MaterialButton action = findViewById(R.id.admin_order_advance);
        action.setVisibility(next == null ? View.GONE : View.VISIBLE);
        action.setText(next == OrderStatus.CONFIRMED ? R.string.admin_order_confirm : R.string.admin_order_complete);
        action.setEnabled(!busy && next != null);
        findViewById(R.id.admin_order_completed).setVisibility(next == null ? View.VISIBLE : View.GONE);
        findViewById(R.id.admin_order_content).setVisibility(View.VISIBLE);
        findViewById(R.id.admin_order_footer).setVisibility(View.VISIBLE);
    }

    private void advance() {
        if (busy || user == null || details == null) return;
        OrderStatus next = OrderUi.next(details.order.status);
        if (next == null) return;
        long actor = user.id, generation = app.session.generation();
        submit(() -> { orders.advanceStatus(actor, orderId, next); return orders.detail(actor, orderId); }, result -> {
            if (generation != app.session.generation()) return;
            bind(result);
            Toast.makeText(this, R.string.admin_order_updated, Toast.LENGTH_SHORT).show();
        }, error -> {
            if (generation != app.session.generation()) return;
            showError(message(error)); reload();
        });
    }

    @Override protected void setBusy(boolean busy) {
        super.setBusy(busy); this.busy = busy;
        View action = findViewById(R.id.admin_order_advance);
        if (action != null) action.setEnabled(!busy && details != null && OrderUi.next(details.order.status) != null);
        View retry = findViewById(R.id.admin_order_retry);
        if (retry != null) retry.setEnabled(!busy);
    }

    private void text(int id, String value) { ((TextView) findViewById(id)).setText(value); }
}
