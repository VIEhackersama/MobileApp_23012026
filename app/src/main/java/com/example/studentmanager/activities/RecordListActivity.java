package com.example.studentmanager.activities;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.example.studentmanager.R;
import com.google.android.material.textfield.TextInputLayout;
import java.util.List;
import java.util.Locale;

abstract class RecordListActivity extends BaseActivity {
    protected final RecordAdapter adapter = new RecordAdapter();

    @Override protected void onCreate(Bundle state) {
        super.onCreate(state);
        setContentView(R.layout.activity_list);
        setupWindow();
        RecyclerView list = findViewById(R.id.list);
        list.setLayoutManager(new LinearLayoutManager(this));
        list.setAdapter(adapter);
        ((EditText) findViewById(R.id.search)).addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) { filter(); }
            @Override public void afterTextChanged(Editable s) {}
        });
    }

    protected void searchHint(int stringId) {
        ((TextInputLayout) findViewById(R.id.search_field)).setHint(getString(stringId));
    }

    protected String query() { return text(findViewById(R.id.root), R.id.search).trim().toLowerCase(Locale.ROOT); }
    protected boolean contains(String value, String query) { return value.toLowerCase(Locale.ROOT).contains(query); }
    protected abstract void filter();

    protected void showRows(List<RecordAdapter.Row> rows) {
        adapter.replace(rows);
        findViewById(R.id.empty).setVisibility(rows.isEmpty() ? View.VISIBLE : View.GONE);
    }

    protected void intro(String value) { ((TextView) findViewById(R.id.list_intro)).setText(value); }
}
