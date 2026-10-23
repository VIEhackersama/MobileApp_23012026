package com.example.fruitshop.activities;

import android.annotation.SuppressLint;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.example.fruitshop.R;
import com.example.fruitshop.core.BusinessClock;
import com.example.fruitshop.core.BusinessDates;
import com.example.fruitshop.core.Money;
import com.example.fruitshop.core.Quantities;
import com.example.fruitshop.models.Fruit;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

final class FruitCatalogAdapter extends RecyclerView.Adapter<FruitCatalogAdapter.Holder> {
    private final List<Fruit> fruits = new ArrayList<>();
    private final Consumer<Fruit> detail, edit, delete;
    private final BusinessClock clock;
    private boolean admin;

    FruitCatalogAdapter(BusinessClock clock, Consumer<Fruit> detail, Consumer<Fruit> edit, Consumer<Fruit> delete) {
        this.clock = clock; this.detail = detail; this.edit = edit; this.delete = delete;
    }
    @SuppressLint("NotifyDataSetChanged") void replace(List<Fruit> values, boolean admin) {
        this.admin = admin; fruits.clear(); fruits.addAll(values); notifyDataSetChanged();
    }
    @NonNull @Override public Holder onCreateViewHolder(@NonNull ViewGroup parent, int type) {
        return new Holder(LayoutInflater.from(parent.getContext()).inflate(R.layout.item_fruit, parent, false));
    }
    @Override public void onBindViewHolder(@NonNull Holder holder, int position) {
        Fruit fruit = fruits.get(position);
        holder.name.setText(fruit.name);
        holder.category.setText(holder.itemView.getContext().getString(R.string.fruit_item_summary,
                fruit.category, Money.format(fruit.price), fruit.unit.label));
        String state = BusinessDates.isExpired(fruit.expiryDate, clock) ? "Hết hạn"
                : fruit.quantity == 0 ? "Hết hàng" : "Còn hàng";
        holder.stock.setText(holder.itemView.getContext().getString(R.string.fruit_item_stock,
                state, Quantities.format(fruit.quantity, fruit.unit), fruit.expiryDate));
        holder.itemView.setOnClickListener(v -> detail.accept(fruit));
        holder.actions.setVisibility(admin ? View.VISIBLE : View.GONE);
        holder.edit.setOnClickListener(v -> edit.accept(fruit));
        holder.delete.setOnClickListener(v -> delete.accept(fruit));
    }
    @Override public int getItemCount() { return fruits.size(); }

    static final class Holder extends RecyclerView.ViewHolder {
        final TextView name, category, stock;
        final View actions, edit, delete;
        Holder(View view) {
            super(view);
            name = view.findViewById(R.id.fruit_item_name);
            category = view.findViewById(R.id.fruit_item_category);
            stock = view.findViewById(R.id.fruit_item_stock);
            actions = view.findViewById(R.id.fruit_item_actions);
            edit = view.findViewById(R.id.fruit_item_edit);
            delete = view.findViewById(R.id.fruit_item_delete);
        }
    }
}
