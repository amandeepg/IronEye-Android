package com.ironeye.android;

import android.app.Fragment;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.SimpleAdapter;
import android.widget.TextView;

import com.ironeye.IronEyeProtos;
import com.nhaarman.listviewanimations.swinginadapters.AnimationAdapter;
import com.nhaarman.listviewanimations.swinginadapters.prepared.SwingRightInAnimationAdapter;

import java.util.ArrayList;
import java.util.Map;

import butterknife.ButterKnife;
import butterknife.InjectView;
import butterknife.OnClick;
import hugo.weaving.DebugLog;

import static com.ironeye.IronEyeProtos.IronMessage.WorkoutInfo;

public class TrackFragment extends Fragment {

    public static final int REAL_TIME_TYPE = 1;
    public static final int HISTORICAL_TYPE = 2;
    private static final String[] FROM = new String[]{"name", "msg"};
    private static final int[] TO = new int[]{android.R.id.text1, android.R.id.text2};
    private final int type;
    public WorkoutInfo workoutInfo;

    @InjectView(R.id.listView)
    ListView lv;

    @InjectView(R.id.sets_info)
    LinearLayout setInfoView;

    @InjectView(R.id.workout_info)
    LinearLayout workoutInfoView;

    @InjectView(R.id.play_video_button)
    Button playVideoBut;

    @InjectView(R.id.controls)
    LinearLayout controlsLay;

    private ArrayList<Map<String, String>> mLst;
    private SimpleAdapter mAdapter;
    private AnimationAdapter mAnimationAdapter;
    private String uid;

    public TrackFragment(int type) {
        this.type = type;
    }

    @DebugLog
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        final View view = inflater.inflate(R.layout.real_time_fragment, container, false);
        ButterKnife.inject(this, view);

        workoutInfoView.setVisibility(type == REAL_TIME_TYPE ? View.GONE : View.VISIBLE);

        controlsLay.setVisibility(type == HISTORICAL_TYPE ? View.GONE : View.VISIBLE);
        lv.setVisibility(type == HISTORICAL_TYPE ? View.GONE : View.VISIBLE);

        if (type == REAL_TIME_TYPE) {
            workoutInfoView.setVisibility(View.GONE);
            controlsLay.setVisibility(View.VISIBLE);
            lv.setVisibility(View.VISIBLE);
            playVideoBut.setEnabled(false);

            mLst = new ArrayList<Map<String, String>>();
            mAdapter = new SimpleAdapter(getActivity(), mLst, R.layout.card_two_item, FROM, TO);

            mAnimationAdapter = new SwingRightInAnimationAdapter(mAdapter);
            mAnimationAdapter.setAbsListView(lv);
            lv.setAdapter(mAnimationAdapter);
        } else if (type == HISTORICAL_TYPE) {
            workoutInfoView.setVisibility(View.VISIBLE);
            controlsLay.setVisibility(View.GONE);
            lv.setVisibility(View.GONE);
            playVideoBut.setEnabled(true);

            displayWorkoutInfo(workoutInfo);
        }

        return view;
    }

    public void refreshListAsync(final ArrayList<Map<String, String>> lst) {
        getActivity().runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mAnimationAdapter.reset();
                refreshList(lst);
            }
        });
    }

    @DebugLog
    public void refreshList(ArrayList<Map<String, String>> lst) {
        mLst.clear();
        mLst.addAll(lst);
        mAdapter.notifyDataSetChanged();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        ButterKnife.reset(this);
    }

    public void displayWorkoutInfoAsync(final WorkoutInfo workoutInfo) {
        getActivity().runOnUiThread(new Runnable() {
            @Override
            public void run() {
                displayWorkoutInfo(workoutInfo);
            }
        });
    }

    public void displayWorkoutInfo(final WorkoutInfo workoutInfo) {
        int i = 1;
        for (IronEyeProtos.IronMessage.Set set : workoutInfo.getSetList()) {
            final View view = getActivity().getLayoutInflater().inflate(R.layout.rep_weight_card, setInfoView, false);

            TextView repView = ButterKnife.findById(view, R.id.rep_count);
            TextView weightView = ButterKnife.findById(view, R.id.weight_for_set);
            TextView setNumView = ButterKnife.findById(view, R.id.set_num);

            repView.setText(String.valueOf(set.getReps()));
            weightView.setText(String.valueOf(set.getWeight()));
            setNumView.setText("Set " + i++);

            setInfoView.addView(view);
        }

        if (type == REAL_TIME_TYPE) {
            workoutInfoView.setVisibility(View.VISIBLE);
            Animation slide = AnimationUtils.loadAnimation(getActivity().getApplicationContext(), R.anim.slide_up);
            workoutInfoView.startAnimation(slide);
        }
    }

    public void setUid(String uid) {
        this.uid = uid;
    }

    public void setVideoReady(final boolean ready) {
        getActivity().runOnUiThread(new Runnable() {
            @Override
            public void run() {
                playVideoBut.setEnabled(ready);
            }
        });
    }

    @OnClick(R.id.play_video_button)
    public void clickPlayVideo() {
        Intent intent = new Intent(Intent.ACTION_VIEW);
        intent.setDataAndType(Uri.fromFile(FileUtils.getDayFile(getActivity(), uid, AppConsts.VIDEO_FILENAME)), "video/*");
        startActivity(intent);
    }
}
