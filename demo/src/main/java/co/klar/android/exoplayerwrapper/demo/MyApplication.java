package co.klar.android.exoplayerwrapper.demo;

import android.app.Application;

import timber.log.Timber;

/**
 * Created by cklar on 27.03.16.
 */
public class MyApplication extends Application {
    @Override
    public void onCreate() {
        super.onCreate();
        Timber.plant(new Timber.DebugTree());

    }
}
