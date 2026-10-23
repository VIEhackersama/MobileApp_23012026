package com.example.fruitshop.activities;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.DiffUtil;
import com.example.fruitshop.R;
import com.example.fruitshop.core.Money;
import com.example.fruitshop.core.Quantities;
import com.example.fruitshop.dto.OrderDetails;
import com.example.fruitshop.models.OrderItem;
import java.util.ArrayList;
import java.util.List;
import java.util.function.LongConsumer;

final class AdminOrderAdapter extends RecyclerView.Adapter<AdminOrderAdapter.Holder> {
    private final List<OrderDetails> rows = new ArrayList<>();
    private final LongConsumer onOpen;

    AdminOrderAdapter(LongConsumer onOpen) { this.onOpen = onOpen; }

    void replace(List<OrderDetails> orders) {
        List<OrderDetails> previous = new ArrayList<>(rows);
        DiffUtil.DiffResult changes = DiffUtil.calculateDiff(new DiffUtil.Callback() {
            @Override public int getOldListSize() { return previous.size(); }
            @Override public int getNewListSize() { return orders.size(); }
            @Override public boolean areItemsTheSame(int oldPosition, int newPosition) {
                return previous.get(oldPosition).order.id == orders.get(newPosition).order.id;
            }
            @Override public boolean areContentsTheSame(int oldPosition, int newPosition) {
                // Detail includes live contact data and item snapshots; rebind an existing card on refresh.
                return previous.get(oldPosition) == orders.get(newPosition);
            }
        });
        rows.clear(); rows.addAll(orders);
        changes.dispatchUpdatesTo(this);
    }

    @NonNull @Override public Holder onCreateViewHolder(@NonNull ViewGroup parent, int type) {
        return new Holder(LayoutInflater.from(parent.getContext()).inflate(R.layout.item_admin_order, parent, false));
    }

    @Override public void onBindViewHolder(@NonNull Holder holder, int position) {
        OrderDetails details = rows.get(position);
        View row = holder.itemView;
        holder.text(R.id.admin_order_number, row.getResources().getString(R.string.admin_order_number, details.order.id));
        holder.text(R.id.admin_order_date, OrderUi.date(details.order.orderDate));
        OrderUi.status(row.findViewById(R.id.admin_order_status), details.order.status);
        holder.text(R.id.admin_order_customer, details.customer == null
                ? row.getResources().getString(R.string.admin_order_customer_unknown) : details.customer.fullname);
        holder.text(R.id.admin_order_contact, details.customer == null ? "" : details.customer.phone);
        holder.text(R.id.admin_order_item_count, row.getResources().getString(R.string.admin_order_item_count, details.items.size()));
        StringBuilder preview = new StringBuilder();
        for (OrderItem item : details.items) {
            if (preview.length() > 0) preview.append("\n");
            preview.append(row.getResources().getString(R.string.admin_order_preview_item,
                    item.fruitName, Quantities.format(item.quantity, item.unit)));
        }
        holder.text(R.id.admin_order_items_preview, preview.toString());
        holder.text(R.id.admin_order_amount, Money.format(details.order.totalAmount));
        row.setOnClickListener(v -> onOpen.accept(details.order.id));
    }

    @Override public int getItemCount() { return rows.size(); }

    static final class Holder extends RecyclerView.ViewHolder {
        Holder(View view) { super(view); }
        void text(int id, String text) { ((TextView) itemView.findViewById(id)).setText(text); }
    }
}
