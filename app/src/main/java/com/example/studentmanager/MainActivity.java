package com.example.studentmanager;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import com.example.studentmanager.activities.BaseActivity;
import com.example.studentmanager.activities.ScoreActivity;
import com.example.studentmanager.activities.StudentActivity;
import com.example.studentmanager.models.Score;
import java.util.List;
import java.util.Locale;

public final class MainActivity extends BaseActivity {
    @Override protected void onCreate(Bundle state) {
        super.onCreate(state);
        setContentView(R.layout.activity_home);
        setupWindow();
        setupToolbar(getString(R.string.app_name), false);
        findViewById(R.id.scores).setEnabled(false);
        findViewById(R.id.students).setOnClickListener(v -> startActivity(new Intent(this, StudentActivity.class)));
        findViewById(R.id.scores).setOnClickListener(v -> startActivity(new Intent(this, ScoreActivity.class)));
    }

    @Override protected void onAuthenticated() {
        boolean teacher = user.isTeacher();
        label(R.id.role, teacher ? "KHÔNG GIAN GIÁO VIÊN" : "KHÔNG GIAN SINH VIÊN");
        label(R.id.greeting, "Xin chào,\n" + user.fullname + ".");
        label(R.id.intro, teacher ? "Theo dõi sinh viên và cập nhật kết quả học tập của lớp."
                : "Theo dõi kết quả của bạn, từng môn học một.");
        label(R.id.profile, (teacher ? "Giáo viên" : "Mã sinh viên: " + user.studentCode)
                + "\n" + user.email + "\nTài khoản: " + user.username);
        findViewById(R.id.students).setVisibility(teacher ? View.VISIBLE : View.GONE);
        findViewById(R.id.scores).setEnabled(true);
        label(R.id.stat_label, teacher ? "SINH VIÊN TRONG DANH SÁCH" : "ĐIỂM TRUNG BÌNH");
        if (teacher) {
            submit(() -> new int[]{app.repository.students(user.id).size(), app.repository.scores(user.id).size()}, result -> {
                label(R.id.stat, Integer.toString(result[0]));
                label(R.id.stat_detail, result[1] + " bản ghi điểm do bạn nhập");
            });
        } else {
            submit(() -> app.repository.scores(user.id), this::showStudentSummary);
        }
    }

    private void showStudentSummary(List<Score> scores) {
        double total = 0;
        for (Score score : scores) total += score.score;
        label(R.id.stat, scores.isEmpty() ? "—" : String.format(Locale.getDefault(), "%.2f", total / scores.size()));
        label(R.id.stat_detail, scores.size() + " bản ghi điểm · trung bình cộng, thang 10");
    }

    private void label(int id, String value) { ((TextView) findViewById(id)).setText(value); }
}
