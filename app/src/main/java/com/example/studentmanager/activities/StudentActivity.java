package com.example.studentmanager.activities;

import android.os.Bundle;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;
import androidx.appcompat.app.AlertDialog;
import com.example.studentmanager.R;
import com.example.studentmanager.models.Student;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import java.util.ArrayList;
import java.util.List;

public final class StudentActivity extends RecordListActivity {
    private List<Student> students = new ArrayList<>();

    @Override protected void onCreate(Bundle state) {
        super.onCreate(state);
        setupToolbar("Sinh viên", true);
        searchHint(R.string.search_students);
        ((TextView) findViewById(R.id.empty)).setText(R.string.empty_students);
        ((TextView) findViewById(R.id.add)).setText(R.string.add_student);
        findViewById(R.id.add).setOnClickListener(v -> edit(null));
    }

    @Override protected void onAuthenticated() {
        if (!user.isTeacher()) { showError("Chức năng này chỉ dành cho giáo viên."); finish(); return; }
        findViewById(R.id.add).setVisibility(View.VISIBLE);
        reload();
    }

    private void reload() {
        submit(() -> app.repository.students(user.id), result -> {
            students = result;
            intro(students.size() + " sinh viên · quản lý thông tin và tài khoản học tập");
            filter();
        });
    }

    @Override protected void filter() {
        List<RecordAdapter.Row> rows = new ArrayList<>();
        String query = query();
        for (Student student : students) {
            if (!contains(student.code + " " + student.fullname + " " + student.email, query)) continue;
            rows.add(new RecordAdapter.Row(student.code, student.fullname,
                    student.email + "\nTài khoản: " + student.username,
                    () -> edit(student), () -> confirmDelete(student)));
        }
        showRows(rows);
    }

    private void edit(Student student) {
        View form = getLayoutInflater().inflate(R.layout.form_student, null);
        if (student != null) {
            ((EditText) form.findViewById(R.id.code)).setText(student.code);
            form.findViewById(R.id.code).setEnabled(false);
            ((EditText) form.findViewById(R.id.fullname)).setText(student.fullname);
            ((EditText) form.findViewById(R.id.email)).setText(student.email);
            form.findViewById(R.id.username_field).setVisibility(View.GONE);
            form.findViewById(R.id.password_field).setVisibility(View.GONE);
        }
        AlertDialog dialog = new MaterialAlertDialogBuilder(this)
                .setTitle(student == null ? "Thêm sinh viên" : "Sửa thông tin sinh viên")
                .setView(form).setNegativeButton(R.string.cancel, null).setPositiveButton(R.string.save, null).create();
        dialog.setOnShowListener(ignored -> dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
            String code = text(form, R.id.code), name = text(form, R.id.fullname), email = text(form, R.id.email);
            String username = text(form, R.id.username), password = text(form, R.id.password);
            long actorId = user.id;
            saveDialog(dialog, form, () -> {
                if (student == null) app.repository.createStudent(actorId, code, name, email, username, password);
                else app.repository.updateStudent(actorId, student.id, name, email);
                return null;
            }, this::reload);
        }));
        dialog.show();
    }

    private void confirmDelete(Student student) {
        new MaterialAlertDialogBuilder(this).setTitle("Xóa " + student.code + "?")
                .setMessage("Chỉ xóa được sinh viên chưa có điểm. Tài khoản đăng nhập cũng sẽ bị xóa.")
                .setNegativeButton(R.string.cancel, null).setPositiveButton(R.string.delete, (dialog, which) ->
                        submit(() -> { app.repository.deleteStudent(user.id, student.id); return null; }, ignored -> reload())).show();
    }
}
