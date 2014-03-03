package com.ironeye.android;

import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.view.Menu;
import android.view.MenuItem;

import com.ironeye.android.utils.FileUtils;

import java.io.FileInputStream;
import java.io.IOException;

import static com.ironeye.IronEyeProtos.IronMessage.WorkoutInfo;

public class ReviewPastActivity extends FragmentActivity {

    public static final String UID = "UID_ARG";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_review_past);

        TrackFragment trackFrag = new TrackFragment(TrackFragment.HISTORICAL_TYPE);

        getFragmentManager().beginTransaction()
                .add(R.id.fragment_container, trackFrag).commit();

        getActionBar().setDisplayHomeAsUpEnabled(true);

        Bundle extras = getIntent().getExtras();
        if (extras != null) {
            String uid = extras.getString(UID);
            try {
                trackFrag.workoutInfo = WorkoutInfo.parseFrom(new FileInputStream(FileUtils.getDayFile(
                        this, uid, AppConsts.WORKOUT_INFO_FILENAME)));
                trackFrag.setUid(uid);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.review_past, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        switch (id) {
            // Respond to the action bar's Up/Home button
            case android.R.id.home:
                finish();
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

}
