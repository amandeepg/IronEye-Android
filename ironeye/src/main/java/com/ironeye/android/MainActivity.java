package com.ironeye.android;

import android.app.ActionBar;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Fragment;
import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.widget.DrawerLayout;
import android.util.Log;
import android.util.SparseArray;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesClient;
import com.google.android.gms.common.Scopes;
import com.google.android.gms.plus.PlusClient;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;

import hugo.weaving.DebugLog;

import static com.ironeye.IronEyeProtos.IronMessage;

public class MainActivity extends Activity
        implements NavigationDrawerFragment.NavigationDrawerCallbacks,
        GooglePlayServicesClient.ConnectionCallbacks,
        GooglePlayServicesClient.OnConnectionFailedListener {

    public static final int SERVER_PORT = 38300;
    private static final String TAG = "MainActivity";
    /**
     * Fragment managing the behaviors, interactions and presentation of the navigation drawer.
     */
    private NavigationDrawerFragment mNavigationDrawerFragment;

    private PlusClient mPlusClient;

    /**
     * Used to store the last screen title. For use in {@link #restoreActionBar()}.
     */
    private CharSequence mTitle;
    private SparseArray<Fragment> mFrags = new SparseArray<Fragment>();

    private Socket mSocket;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mPlusClient = new PlusClient.Builder(this, this, this)
                .setScopes(Scopes.PLUS_PROFILE)
                .build();

        setContentView(R.layout.main_activity);

        mNavigationDrawerFragment = (NavigationDrawerFragment)
                getFragmentManager().findFragmentById(R.id.navigation_drawer);
        mTitle = getTitle();

        // Set up the drawer.
        mNavigationDrawerFragment.setUp(R.id.navigation_drawer,
                (DrawerLayout) findViewById(R.id.drawer_layout));

        startServerSocket();
    }

    private void startServerSocket() {
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    receiveFromServer();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }).start();
    }

    private void receiveFromServer() throws IOException {
        ServerSocket mServerSocket = new ServerSocket(SERVER_PORT);
        mSocket = mServerSocket.accept();

        IronMessage.UserInfo.Builder userInfo = IronMessage.UserInfo.newBuilder()
                .setId(AppController.getInstance().currentPerson.getId());

        IronMessage msg = IronMessage.newBuilder()
                .setUserInfo(userInfo)
                .setType(IronMessage.MessageType.USER_INFO)
                .build();

        OutputStream out = mSocket.getOutputStream();
        InputStream in = mSocket.getInputStream();

        msg.writeDelimitedTo(out);
        String uid = String.valueOf(System.currentTimeMillis());
        Log.d(TAG, "uid = " + uid);

        boolean mkdirs = new File(getExternalFilesDir(null), uid).mkdirs();
        if (!mkdirs) {
            Log.d(TAG, "Directory not made.");
        }

        int i = 0;

        while (true) {
            IronMessage statusMsg = IronMessage.parseDelimitedFrom(in);
            if (statusMsg.getType().equals(IronMessage.MessageType.WORKOUT_INFO)) {
                IronMessage.WorkoutInfo workoutInfo = statusMsg.getWorkoutInfo();
                StringBuilder toastMessage = new StringBuilder();

                for (IronMessage.Set set : workoutInfo.getSetList()) {
                    int reps = set.getReps();
                    int weight = set.getWeight();
                    Log.d(TAG, "reps = " + reps);
                    Log.d(TAG, "weight = " + weight);

                    toastMessage
                            .append("Reps: ").append(reps).append(", ")
                            .append("Weight: ").append(weight).append("\n");
                }

                showToast(toastMessage);

                FileOutputStream fos = new FileOutputStream(new File(getExternalFilesDir(null), uid + File.separator + "workout_info"));
                workoutInfo.writeTo(fos);
                fos.close();
                break;
            }

            if (i++ < 0) {
                Log.d(TAG, "SKIP " + i);
                continue;
            } else {
                i = 0;
            }

            IronMessage.FormErrorData formError = statusMsg.getErrorData();
            StringBuilder toastMessage = new StringBuilder();
            for (IronMessage.JointError je : formError.getJointList()) {
                String jointType = je.getJointType().toString();
                String jointMsg = je.getErrorMessage();
                Log.d(TAG, "status = " + jointType + " - " + jointMsg);
                toastMessage
                        .append(jointType).append(": ")
                        .append(jointMsg).append("\n");
            }

            showToast(toastMessage);
        }

        File vidFile = new File(getExternalFilesDir(null), uid + File.separator + "video.mp4");
        inputStreamToFile(in, vidFile);

        mSocket.close();
        mServerSocket.close();

        promptPlayVideo(vidFile);

        final LogFragment logFrag = (LogFragment) getFragType(LogFragment.class);
        if (logFrag != null) {
            logFrag.refreshGraphAsync();
        }

        startServerSocket();
    }

    private Fragment getFragType(Class clss) {
        for (int k = 0; k < mFrags.size(); k++) {
            Fragment fr = mFrags.valueAt(k);
            if (fr.getClass().equals(clss)) {
                return fr;
            }
        }
        return null;
    }

    private void inputStreamToFile(InputStream in, File vidFile) throws IOException {
        FileOutputStream fos = new FileOutputStream(vidFile);

        int read;
        byte[] bytes = new byte[1024];
        while ((read = in.read(bytes)) != -1) {
            fos.write(bytes, 0, read);
        }

        fos.close();
    }

    private void promptPlayVideo(final File vidFile) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                final DialogInterface.OnClickListener dialogClickListener = new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        switch (which) {
                            case DialogInterface.BUTTON_POSITIVE:
                                Intent intent = new Intent(Intent.ACTION_VIEW);
                                intent.setDataAndType(Uri.fromFile(vidFile), "video/*");
                                startActivity(intent);
                                break;

                            case DialogInterface.BUTTON_NEGATIVE:
                                break;
                        }
                    }
                };

                AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
                builder.setMessage("Play Video?").setPositiveButton("Yes", dialogClickListener)
                        .setNegativeButton("No", dialogClickListener).show();
            }
        });
    }

    private void showToast(final StringBuilder toastMessage) {
        if (toastMessage.length() < 1) {
            return;
        }
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(MainActivity.this, toastMessage.toString(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    public void onNavigationDrawerItemSelected(int position) {
        FragmentManager fragmentManager = getFragmentManager();
        Fragment frag = mFrags.get(position);
        FragmentTransaction ft = fragmentManager.beginTransaction();

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
                    frag = PlaceholderFragment.newInstance(position);
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
            Fragment fr = mFrags.valueAt(i);
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
        ActionBar actionBar = getActionBar();
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
            case R.id.start_set_action:
                sendSetControlMsg(IronMessage.MessageType.SET_START);
                break;
            case R.id.end_set_action:
                sendSetControlMsg(IronMessage.MessageType.SET_END);
                break;
        }

        return super.onOptionsItemSelected(item);
    }

    private void sendSetControlMsg(final IronMessage.MessageType type) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                IronMessage msg = IronMessage.newBuilder()
                        .setType(type)
                        .build();

                try {
                    msg.writeDelimitedTo(mSocket.getOutputStream());
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }).start();
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

    /**
     * A placeholder fragment containing a simple view.
     */
    public static class PlaceholderFragment extends Fragment {
        /**
         * The fragment argument representing the section number for this
         * fragment.
         */
        private static final String ARG_SECTION_NUMBER = "section_number";

        public PlaceholderFragment() {
        }

        /**
         * Returns a new instance of this fragment for the given section
         * number.
         */
        public static PlaceholderFragment newInstance(int sectionNumber) {
            PlaceholderFragment fragment = new PlaceholderFragment();
            Bundle args = new Bundle();
            args.putInt(ARG_SECTION_NUMBER, sectionNumber);
            fragment.setArguments(args);
            return fragment;
        }

        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container,
                                 Bundle savedInstanceState) {
            View rootView = inflater.inflate(R.layout.main_placeholder_fragment, container, false);
            TextView textView = (TextView) rootView.findViewById(R.id.section_label);
            textView.setText(Integer.toString(getArguments().getInt(ARG_SECTION_NUMBER)));
            return rootView;
        }
    }

}
