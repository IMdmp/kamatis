package com.dmp.project.kamatis2;

import android.app.Application;


import timber.log.Timber;

public class SampleApplication extends Application {
    @Override
    public void onCreate() {
        super.onCreate();


        if(BuildConfig.DEBUG){
            Timber.plant(new Timber.DebugTree());
        }
    }
}
