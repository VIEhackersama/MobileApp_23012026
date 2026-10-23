package com.example.fruitshop;

import android.app.Application;
import android.os.Handler;
import android.os.Looper;
import com.example.fruitshop.core.BusinessClock;
import com.example.fruitshop.dal.AuthRepository;
import com.example.fruitshop.dal.DBHelper;
import com.example.fruitshop.session.SessionManager;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public final class FruitShopApp extends Application {
    public DBHelper database;
    public AuthRepository auth;
    public SessionManager session;
    public final BusinessClock clock = BusinessClock.system();
    public final ExecutorService databaseExecutor = Executors.newSingleThreadExecutor();
    public final Handler main = new Handler(Looper.getMainLooper());
    @Override public void onCreate() {
        super.onCreate();
        database = new DBHelper(this, DBHelper.DATABASE_NAME, clock);
        auth = new AuthRepository(database); session = new SessionManager(this);
    }
}
