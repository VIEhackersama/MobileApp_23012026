package com.example.studentmanager;

import android.app.Application;
import android.os.Handler;
import android.os.Looper;
import com.example.studentmanager.dal.DBHelper;
import com.example.studentmanager.dal.StudentRepository;
import com.example.studentmanager.session.SessionManager;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public final class StudentManagerApp extends Application {
    public StudentRepository repository;
    public SessionManager session;
    public final ExecutorService databaseExecutor = Executors.newSingleThreadExecutor();
    public final Handler main = new Handler(Looper.getMainLooper());

    @Override public void onCreate() {
        super.onCreate();
        repository = new StudentRepository(new DBHelper(this));
        session = new SessionManager(this);
    }
}
