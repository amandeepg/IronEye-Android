package com.ironeye.android;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesClient;
import com.google.android.gms.common.Scopes;
import com.google.android.gms.plus.PlusClient;

import hugo.weaving.DebugLog;

public class SplashActivity extends Activity implements
        GooglePlayServicesClient.ConnectionCallbacks,
        GooglePlayServicesClient.OnConnectionFailedListener {

    public static final int SPLASH_SHOW_TIME_MILLIS = 2000;
    private static final String TAG = "SplashActivity";

    private PlusClient mPlusClient;
    private long mStartedTime;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.splash_activity);

        mPlusClient = new PlusClient.Builder(this, this, this)
                .setScopes(Scopes.PLUS_PROFILE)
                .build();
        mStartedTime = System.currentTimeMillis();
    }

    private void finishSplash(final Class<?> cls) {
        final long timeDiff = System.currentTimeMillis() - mStartedTime;
        final long startDelay = timeDiff > SPLASH_SHOW_TIME_MILLIS ? 0 : SPLASH_SHOW_TIME_MILLIS - timeDiff;
        Log.d(TAG, "Launching " + cls + " in " + startDelay + "ms. ");

        Runnable r = new Runnable() {

            @Override
            public void run() {
                final Intent intent = new Intent(SplashActivity.this, cls);
                startActivity(intent);
                finish();
            }
        };

        if (startDelay <= 0) {
            r.run();
        } else {
            new Handler().postDelayed(r, startDelay);
        }
    }

    @DebugLog
    @Override
    public void onConnected(Bundle bundle) {
        AppController.getInstance().currentPerson = mPlusClient.getCurrentPerson();
        finishSplash(MainActivity.class);
    }

    @DebugLog
    @Override
    public void onDisconnected() {
        finishSplash(SignInActivity.class);
    }

    @DebugLog
    @Override
    public void onConnectionFailed(ConnectionResult connectionResult) {
        onDisconnected();
    }

    @Override
    protected void onStart() {
        super.onStart();
        mPlusClient.connect();
    }

    @Override
    protected void onStop() {
        super.onStop();
        mPlusClient.disconnect();
    }
}
