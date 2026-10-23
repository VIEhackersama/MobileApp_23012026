package com.example.studentmanager.activities;

import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import androidx.appcompat.app.AlertDialog;
import com.example.studentmanager.R;
import com.example.studentmanager.models.Score;
import com.example.studentmanager.models.Student;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.text.NumberFormat;

public final class ScoreActivity extends RecordListActivity {
    private List<Score> scores = new ArrayList<>();

    @Override protected void onCreate(Bundle state) {
        super.onCreate(state);
        setupToolbar("Bảng điểm", true);
        searchHint(R.string.search_scores);
        ((TextView) findViewById(R.id.empty)).setText(R.string.empty_scores);
        ((TextView) findViewById(R.id.add)).setText(R.string.add_score);
        findViewById(R.id.add).setOnClickListener(v -> prepareEdit(null));
    }

    @Override protected void onAuthenticated() {
        findViewById(R.id.add).setVisibility(user.isTeacher() ? View.VISIBLE : View.GONE);
        reload();
    }

    private void reload() {
        submit(() -> app.repository.scores(user.id), result -> {
            scores = result;
            intro(scores.size() + " bản ghi · " + (user.isTeacher() ? "điểm do bạn nhập" : "kết quả học tập của bạn"));
            filter();
        });
    }

    @Override protected void filter() {
        List<RecordAdapter.Row> rows = new ArrayList<>();
        String query = query();
        for (Score score : scores) {
            if (!contains(score.subject + " " + score.studentCode + " " + score.studentName, query)) continue;
            boolean teacher = user != null && user.isTeacher();
            rows.add(new RecordAdapter.Row(score.studentCode + " · " + score.studentName,
                    score.subject + "  /  " + String.format(Locale.getDefault(), "%.1f", score.score),
                    "Giáo viên: " + score.teacherName + "\nThang điểm: 10",
                    teacher ? () -> prepareEdit(score) : null, teacher ? () -> confirmDelete(score) : null));
        }
        showRows(rows);
    }

    private void prepareEdit(Score score) {
        submit(() -> app.repository.students(user.id), students -> {
            if (students.isEmpty()) { showError("Hãy thêm sinh viên trước khi nhập điểm."); return; }
            edit(score, students);
        });
    }

    private void edit(Score score, List<Student> students) {
        View form = getLayoutInflater().inflate(R.layout.form_score, null);
        Spinner spinner = form.findViewById(R.id.student_spinner);
        ArrayAdapter<Student> choices = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, students);
        choices.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinner.setAdapter(choices);
        if (score != null) {
            for (int i = 0; i < students.size(); i++) if (students.get(i).code.equals(score.studentCode)) spinner.setSelection(i);
            ((EditText) form.findViewById(R.id.subject)).setText(score.subject);
            // Keep all entered precision when editing; displayed list rounding must not alter saved data.
            NumberFormat number = NumberFormat.getNumberInstance(Locale.US);
            number.setGroupingUsed(false);
            number.setMaximumFractionDigits(340);
            ((EditText) form.findViewById(R.id.grade)).setText(number.format(score.score));
        }
        AlertDialog dialog = new MaterialAlertDialogBuilder(this).setTitle(score == null ? "Nhập điểm" : "Sửa điểm")
                .setView(form).setNegativeButton(R.string.cancel, null).setPositiveButton(R.string.save, null).create();
        dialog.setOnShowListener(ignored -> dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
            String code = ((Student) spinner.getSelectedItem()).code;
            String subject = text(form, R.id.subject);
            String grade = text(form, R.id.grade).trim().replace(',', '.');
            long actorId = user.id;
            saveDialog(dialog, form, () -> {
                double number;
                try { number = Double.parseDouble(grade); }
                catch (NumberFormatException e) { throw new IllegalArgumentException("Vui lòng nhập điểm dạng số từ 0 đến 10."); }
                app.repository.saveScore(actorId, score == null ? 0 : score.id, code, subject, number);
                return null;
            }, this::reload);
        }));
        dialog.show();
    }

    private void confirmDelete(Score score) {
        new MaterialAlertDialogBuilder(this).setTitle("Xóa điểm môn " + score.subject + "?")
                .setMessage(score.studentCode + " · " + score.studentName)
                .setNegativeButton(R.string.cancel, null).setPositiveButton(R.string.delete, (dialog, which) ->
                        submit(() -> { app.repository.deleteScore(user.id, score.id); return null; }, ignored -> reload())).show();
    }
}
