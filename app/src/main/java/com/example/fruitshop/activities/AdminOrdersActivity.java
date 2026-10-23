package com.example.fruitshop.activities;

import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import com.example.fruitshop.R;
import com.example.fruitshop.contracts.OrderDataSource;
import com.example.fruitshop.core.Money;
import com.example.fruitshop.core.Quantities;
import com.example.fruitshop.dto.OrderDetails;
import com.example.fruitshop.models.Order;
import com.example.fruitshop.models.OrderItem;
import com.example.fruitshop.models.OrderStatus;
import com.example.fruitshop.models.Role;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/** Admin UI uses Person 3's OrderDataSource for all authorization and transitions. */
public final class AdminOrdersActivity extends BaseActivity {
    public interface OrderProvider { OrderDataSource orderDataSource(); }

    private OrderDataSource orders;
    private List<Order> current = new ArrayList<>();
    private ArrayAdapter<String> adapter;

    @Override protected Role requiredRole() { return Role.ADMIN; }

    @Override protected void onCreate(Bundle state) {
        super.onCreate(state);
        setContentView(R.layout.activity_admin_orders);
        setupWindow();
        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        toolbar.setTitle(R.string.fruit_admin_orders);
        toolbar.setNavigationIcon(R.drawable.ic_back);
        toolbar.setNavigationContentDescription(R.string.back);
        toolbar.setNavigationOnClickListener(v -> finish());
        adapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, new ArrayList<>());
        ListView list = findViewById(R.id.admin_order_list);
        list.setAdapter(adapter);
        list.setOnItemClickListener((parent, view, position, id) -> showDetails(current.get(position)));
    }

    @Override protected void onAuthenticated() {
        if (!(getApplication() instanceof OrderProvider)) {
            showError(getString(R.string.fruit_orders_unavailable));
            finish();
            return;
        }
        orders = ((OrderProvider) getApplication()).orderDataSource();
        reload();
    }

    private void reload() {
        long actorId = user.id, generation = app.session.generation();
        submit(() -> orders.list(actorId), result -> {
            if (generation != app.session.generation()) return;
            current = result;
            adapter.clear();
            for (Order order : result) adapter.add(getString(R.string.fruit_order_row, order.id,
                    DateFormat.getDateTimeInstance().format(new Date(order.orderDate)),
                    Money.format(order.totalAmount), order.status.name()));
            adapter.notifyDataSetChanged();
            findViewById(R.id.admin_order_empty).setVisibility(result.isEmpty() ? View.VISIBLE : View.GONE);
        });
    }

    private void showDetails(Order row) {
        long actorId = user.id;
        submit(() -> orders.detail(actorId, row.id), details -> showDialog(details));
    }

    private void showDialog(OrderDetails details) {
        Order order = details.order;
        StringBuilder message = new StringBuilder();
        message.append(DateFormat.getDateTimeInstance().format(new Date(order.orderDate))).append('\n');
        message.append(getString(R.string.fruit_order_status, order.status.name())).append("\n\n");
        for (OrderItem item : details.items) {
            message.append(item.fruitName).append(" · ")
                    .append(Quantities.format(item.quantity, item.unit)).append(" · ")
                    .append(Money.format(item.lineTotal)).append('\n');
        }
        message.append('\n').append(getString(R.string.fruit_order_total, Money.format(order.totalAmount)));
        OrderStatus next = order.status == OrderStatus.PENDING ? OrderStatus.CONFIRMED
                : order.status == OrderStatus.CONFIRMED ? OrderStatus.COMPLETED : null;
        MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(this)
                .setTitle(getString(R.string.fruit_order_title, order.id)).setMessage(message.toString())
                .setNegativeButton(R.string.fruit_close, null);
        if (next != null) builder.setPositiveButton(getString(R.string.fruit_advance_status, next.name()),
                (dialog, which) -> submit(() -> { orders.advanceStatus(user.id, order.id, next); return null; },
                        ignored -> reload()));
        builder.show();
    }
}
