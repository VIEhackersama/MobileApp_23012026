package com.example.fruitshop.activities;

import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;
import androidx.appcompat.app.AlertDialog;
import com.example.fruitshop.R;
import com.example.fruitshop.core.Money;
import com.example.fruitshop.core.Quantities;
import com.example.fruitshop.models.Order;
import com.example.fruitshop.models.OrderItem;
import com.example.fruitshop.models.OrderStatus;
import com.example.fruitshop.models.Role;
import com.google.android.material.appbar.MaterialToolbar;
import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/** Owner: person 3. Customer order history and details; Admin manages orders in AdminOrdersActivity. */
public final class OrderListActivity extends BaseActivity {
    private ListView list;
    private List<Order> orders = new ArrayList<>();

    @Override protected Role requiredRole() { return Role.CUSTOMER; }

    @Override protected void onCreate(Bundle state) {
        super.onCreate(state);
        setContentView(R.layout.activity_orders);
        setupWindow();
        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        toolbar.setTitle(R.string.orders_title);
        toolbar.setNavigationIcon(R.drawable.ic_back);
        toolbar.setNavigationContentDescription(R.string.back);
        toolbar.setNavigationOnClickListener(v -> finish());
        ((TextView) findViewById(R.id.orders_empty)).setText(R.string.orders_empty);
        list = findViewById(R.id.orders_list);
        list.setEmptyView(findViewById(R.id.orders_empty));
        list.setOnItemClickListener((parent, view, position, id) -> showDetail(orders.get(position).id));
    }

    @Override protected void onAuthenticated() { reload(); }

    private void reload() {
        long actor = user.id;
        submit(() -> app.orders.list(actor), result -> {
            orders = result;
            DateFormat format = DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT);
            List<String> text = new ArrayList<>();
            for (Order order : result) {
                text.add(getString(R.string.order_row, order.id, format.format(new Date(order.orderDate)),
                        statusLabel(order.status), Money.format(order.totalAmount)));
            }
            list.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, text));
            ((TextView) findViewById(R.id.orders_summary)).setText(getString(R.string.orders_count, result.size()));
        });
    }

    /** Items come from OrderItem snapshots, so later price/name changes or deleted fruits do not alter history. */
    private void showDetail(long orderId) {
        long actor = user.id;
        submit(() -> app.orders.detail(actor, orderId), details -> {
            StringBuilder body = new StringBuilder();
            for (OrderItem item : details.items) {
                body.append(getString(R.string.order_item_row, item.fruitName, Quantities.format(item.quantity, item.unit),
                        Money.format(item.unitPrice), item.unit.label, Money.format(item.lineTotal))).append("\n\n");
            }
            body.append(getString(R.string.order_total, Money.format(details.order.totalAmount)));
            new AlertDialog.Builder(this)
                    .setTitle(getString(R.string.order_detail_title, details.order.id, statusLabel(details.order.status)))
                    .setMessage(body)
                    .setNegativeButton(R.string.order_close, null)
                    .show();
        });
    }

    private String statusLabel(OrderStatus status) {
        switch (status) {
            case PENDING: return getString(R.string.status_pending);
            case CONFIRMED: return getString(R.string.status_confirmed);
            default: return getString(R.string.status_completed);
        }
    }
}
