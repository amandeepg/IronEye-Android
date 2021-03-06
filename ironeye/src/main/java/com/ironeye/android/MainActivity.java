package com.ironeye.android;

import android.app.ActionBar;
import android.app.Activity;
import android.app.Fragment;
import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.content.Intent;
import android.os.Bundle;
import android.speech.tts.TextToSpeech;
import android.support.v4.widget.DrawerLayout;
import android.util.Log;
import android.util.SparseArray;
import android.view.Menu;
import android.view.MenuItem;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesClient;
import com.google.android.gms.common.Scopes;
import com.google.android.gms.plus.PlusClient;

import java.util.ArrayList;
import java.util.Locale;
import java.util.Map;

import butterknife.ButterKnife;
import butterknife.InjectView;
import hugo.weaving.DebugLog;

import static com.ironeye.IronEyeProtos.IronMessage;

public class MainActivity extends Activity
        implements NavigationDrawerFragment.NavigationDrawerCallbacks,
        GooglePlayServicesClient.ConnectionCallbacks,
        GooglePlayServicesClient.OnConnectionFailedListener, TextToSpeech.OnInitListener {

    public TextToSpeech tts;
    public ServerCommThread serverComms;
    @InjectView(R.id.drawer_layout)
    DrawerLayout mDrawerLayout;
    private NavigationDrawerFragment mNavigationDrawerFragment;
    private PlusClient mPlusClient;
    /**
     * Used to store the last screen title. For use in {@link #restoreActionBar()}.
     */
    private CharSequence mTitle;
    private SparseArray<Fragment> mFrags = new SparseArray<Fragment>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mPlusClient = new PlusClient.Builder(this, this, this)
                .setScopes(Scopes.PLUS_PROFILE)
                .build();

        setContentView(R.layout.main_activity);
        ButterKnife.inject(this);

        mNavigationDrawerFragment = (NavigationDrawerFragment)
                getFragmentManager().findFragmentById(R.id.navigation_drawer);
        mNavigationDrawerFragment.setUp(R.id.navigation_drawer, mDrawerLayout);

        mTitle = getTitle();

        tts = new TextToSpeech(this, this);

        startServerSocket();
    }

    public void startServerSocket() {
        serverComms = new ServerCommThread(this);
        serverComms.start();
    }

    @Override
    public void onInit(int status) {
        if (status == TextToSpeech.SUCCESS) {
            final int result = tts.setLanguage(Locale.US);

            if (result == TextToSpeech.LANG_MISSING_DATA
                    || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                Log.e("TTS", "This Language is not supported");
            }

        } else {
            Log.e("TTS", "Initilization Failed!");
        }
    }

    private Fragment getFragType(Class clss) {
        for (int k = 0; k < mFrags.size(); k++) {
            final Fragment fr = mFrags.valueAt(k);
            if (fr.getClass().equals(clss)) {
                return fr;
            }
        }
        return null;
    }

    public void refreshLogGraph() {
        final LogFragment logFrag = getLogFragment();
        if (logFrag != null) {
            logFrag.refreshGraphAsync();
        }
    }

    public void refreshTrackingList(final ArrayList<Map<String, String>> jointListData) {
        final TrackFragment trackFrag = getTrackFragment();
        if (trackFrag != null) {
            trackFrag.refreshListAsync(jointListData);
        }
    }

    public void addWorkoutInfoToTrackingFrag(final IronMessage.WorkoutInfo workoutInfo) {
        final TrackFragment trackFrag = getTrackFragment();
        if (trackFrag != null) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    trackFrag.displayWorkoutInfo(workoutInfo);
                    trackFrag.setSetControlFromServer(true);
                    trackFrag.onExerciseOver();
                }
            });
        }
    }

    public void onRep(final int rep) {
        final TrackFragment trackFrag = getTrackFragment();
        if (trackFrag != null) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    trackFrag.onRepCompleted(rep);
                }
            });
        }
    }

    public void videoReady(final String uid) {
        final TrackFragment trackFrag = getTrackFragment();
        if (trackFrag != null) {
            trackFrag.setUid(uid);
            trackFrag.setVideoReady(true);
        }
    }

    public void onExerciseStarted() {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mNavigationDrawerFragment.selectItem(1);

                final TrackFragment trackFrag = getTrackFragment();
                if (trackFrag != null) {
                    trackFrag.onExerciseStarted();
                }
            }
        });
    }

    @DebugLog
    public void moveControls(final IronMessage msg) {
        final TrackFragment trackFrag = getTrackFragment();
        final IronMessage.MessageType type = msg.getType();
        if (trackFrag != null) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    trackFrag.setSetControlFromServer(true);
                    switch (type) {
                        case SET_START:
                            trackFrag.setCurrentControlItem(0);
                            break;
                        case SET_END:
                            trackFrag.setCurrentControlItem(1);
                            break;
                        case EXERCISE_END:
                            trackFrag.setCurrentControlItem(2);
                            break;
                        default:
                            trackFrag.setSetControlFromServer(false);
                    }
                }
            });
        }
    }

    private TrackFragment getTrackFragment() {
        return (TrackFragment) getFragType(TrackFragment.class);
    }

    private LogFragment getLogFragment() {
        return (LogFragment) getFragType(LogFragment.class);
    }

    @Override
    public void onNavigationDrawerItemSelected(int position) {
        final FragmentManager fragmentManager = getFragmentManager();
        final FragmentTransaction ft = fragmentManager.beginTransaction();

        Fragment frag = mFrags.get(position);

        hideFrags(frag, ft);

        if (frag == null) {
            switch (position) {
                case 0:
                    frag = QrCodeFragment.newInstance();
                    break;
                case 2:
                    frag = LogFragment.newInstance();
                    break;
                default:
                    frag = new TrackFragment(TrackFragment.REAL_TIME_TYPE);
            }
            mFrags.put(position, frag);
            ft.add(R.id.container, frag);
        } else {
            ft.show(frag);
        }
        ft.commit();

        onSectionAttached(position);
        restoreActionBar();
    }

    private void hideFrags(Fragment exemptFrag, FragmentTransaction ft) {
        for (int i = 0; i < mFrags.size(); i++) {
            final Fragment fr = mFrags.valueAt(i);
            if (fr != exemptFrag) {
                ft.hide(fr);
            }
        }
    }

    public void onSectionAttached(int number) {
        switch (number) {
            case 0:
                mTitle = getString(R.string.title_section1);
                break;
            case 1:
                mTitle = getString(R.string.title_section2);
                break;
            case 2:
                mTitle = getString(R.string.title_section3);
                break;
        }
    }

    public void restoreActionBar() {
        final ActionBar actionBar = getActionBar();
        actionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_STANDARD);
        actionBar.setDisplayShowTitleEnabled(true);
        actionBar.setTitle(mTitle);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // if drawer closed
        if (!mNavigationDrawerFragment.isDrawerOpen()) {
            restoreActionBar();
        }
        getMenuInflater().inflate(R.menu.main, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.

        switch (item.getItemId()) {
            case R.id.sign_out_action:
                if (mPlusClient.isConnected()) {
                    mPlusClient.clearDefaultAccount();
                    mPlusClient.disconnect();
                    onDisconnected();
                } else {
                    throw new IllegalStateException("Not connected when trying to sign out.");
                }
                break;
        }

        return super.onOptionsItemSelected(item);
    }

    @DebugLog
    @Override
    public void onConnected(Bundle bundle) {
        AppController.getInstance().currentPerson = mPlusClient.getCurrentPerson();
    }

    @DebugLog
    @Override
    public void onDisconnected() {
        startActivity(new Intent(this, SignInActivity.class));
        finish();
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
