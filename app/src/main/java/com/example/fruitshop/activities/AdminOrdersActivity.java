package com.example.fruitshop.activities;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.example.fruitshop.R;
import com.example.fruitshop.contracts.OrderDataSource;
import com.example.fruitshop.dto.OrderDetails;
import com.example.fruitshop.models.Order;
import com.example.fruitshop.models.OrderStatus;
import com.example.fruitshop.models.Role;
import com.google.android.material.appbar.MaterialToolbar;
import java.util.ArrayList;
import java.util.List;

public final class AdminOrdersActivity extends BaseActivity {
    public interface OrderProvider { OrderDataSource orderDataSource(); }
    private OrderDataSource orders;
    private AdminOrderAdapter adapter;

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
        adapter = new AdminOrderAdapter(orderId -> startActivity(new Intent(this, AdminOrderDetailActivity.class)
                .putExtra(AdminOrderDetailActivity.EXTRA_ORDER_ID, orderId)));
        RecyclerView list = findViewById(R.id.admin_order_list);
        list.setLayoutManager(new LinearLayoutManager(this));
        list.setAdapter(adapter);
        findViewById(R.id.admin_order_retry).setOnClickListener(v -> { if (user != null) reload(); });
    }

    @Override protected void onAuthenticated() {
        if (!(getApplication() instanceof OrderProvider)) {
            showError(getString(R.string.fruit_orders_unavailable)); finish(); return;
        }
        orders = ((OrderProvider) getApplication()).orderDataSource();
        reload();
    }

    private void reload() {
        long actor = user.id, generation = app.session.generation();
        findViewById(R.id.admin_order_error).setVisibility(View.GONE);
        submit(() -> {
            List<OrderDetails> result = new ArrayList<>();
            for (Order order : orders.list(actor)) result.add(orders.detail(actor, order.id));
            return result;
        }, result -> {
            if (generation != app.session.generation()) return;
            adapter.replace(result);
            long pending = 0;
            for (OrderDetails details : result) if (details.order.status == OrderStatus.PENDING) pending++;
            ((TextView) findViewById(R.id.admin_orders_summary)).setText(
                    getString(R.string.admin_orders_count, result.size(), pending));
            findViewById(R.id.admin_order_empty).setVisibility(result.isEmpty() ? View.VISIBLE : View.GONE);
        }, error -> {
            if (generation != app.session.generation()) return;
            findViewById(R.id.admin_order_empty).setVisibility(View.GONE);
            ((TextView) findViewById(R.id.admin_order_error_message)).setText(
                    getString(R.string.admin_orders_load_error, message(error)));
            findViewById(R.id.admin_order_error).setVisibility(View.VISIBLE);
        });
    }
}
