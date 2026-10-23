package com.example.studentmanager.activities;

import android.annotation.SuppressLint;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.example.studentmanager.R;
import java.util.ArrayList;
import java.util.List;

final class RecordAdapter extends RecyclerView.Adapter<RecordAdapter.Holder> {
    static final class Row {
        final String tag, title, detail;
        final Runnable edit, delete;
        Row(String tag, String title, String detail, Runnable edit, Runnable delete) {
            this.tag = tag; this.title = title; this.detail = detail; this.edit = edit; this.delete = delete;
        }
    }
    private final List<Row> rows = new ArrayList<>();

    @SuppressLint("NotifyDataSetChanged")
    void replace(List<Row> replacement) {
        rows.clear(); rows.addAll(replacement); notifyDataSetChanged();
    }

    @NonNull @Override public Holder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new Holder(LayoutInflater.from(parent.getContext()).inflate(R.layout.item_record, parent, false));
    }

    @Override public void onBindViewHolder(@NonNull Holder holder, int position) {
        Row row = rows.get(position);
        ((TextView) holder.itemView.findViewById(R.id.item_tag)).setText(row.tag);
        ((TextView) holder.itemView.findViewById(R.id.item_title)).setText(row.title);
        ((TextView) holder.itemView.findViewById(R.id.item_detail)).setText(row.detail);
        holder.itemView.findViewById(R.id.item_actions).setVisibility(row.edit == null ? View.GONE : View.VISIBLE);
        holder.itemView.findViewById(R.id.item_edit).setOnClickListener(v -> { if (row.edit != null) row.edit.run(); });
        holder.itemView.findViewById(R.id.item_delete).setOnClickListener(v -> { if (row.delete != null) row.delete.run(); });
    }

    @Override public int getItemCount() { return rows.size(); }
    static final class Holder extends RecyclerView.ViewHolder { Holder(View view) { super(view); } }
}
