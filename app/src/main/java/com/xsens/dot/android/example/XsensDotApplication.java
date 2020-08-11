package com.xsens.dot.android.example;

import android.app.Application;
import android.util.Log;

import com.xsens.dot.android.sdk.XsensDotSdk;

public class XsensDotApplication extends Application {

    private static final String TAG = XsensDotApplication.class.getSimpleName();

    @Override
    public void onCreate() {
        super.onCreate();

        initXsensDotSdk();
    }

    private void initXsensDotSdk() {
        Log.d(TAG, "XsensDotSdk version " + XsensDotSdk.getSdkVersion());

        XsensDotSdk.setDebugEnabled(true);
        XsensDotSdk.setReconnectEnabled(true);
    }

}
