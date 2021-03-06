package com.ironeye.android;

import com.ironeye.android.holograph.Bar;
import com.ironeye.android.holograph.BarGraph;
import com.ironeye.android.holograph.YAxisView;
import com.ironeye.android.utils.FileUtils;

import android.app.Fragment;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.util.SparseArray;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;

import butterknife.ButterKnife;
import butterknife.InjectView;
import hugo.weaving.DebugLog;

import static com.ironeye.IronEyeProtos.IronMessage;

public class LogFragment extends Fragment {

    private static final float BAR_PADDING = 1.5f;

    private static final int BAR_SIZE = 150;

    private static final String TAG = "LogFragment";

    private final Runnable refreshGraphTask = new Runnable() {
        @Override
        public void run() {
            refreshGraph();
        }
    };

    @InjectView(R.id.graph)
    BarGraph bg;

    @InjectView(R.id.y_axis_view)
    YAxisView yaxisView;

    private SparseArray<String> workoutUids;

    public LogFragment() {
    }

    /**
     * Returns a new instance of this fragment for the given section
     * number.
     */
    public static LogFragment newInstance() {
        return new LogFragment();
    }

    @DebugLog
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        final View view = inflater.inflate(R.layout.log_fragment, container, false);
        ButterKnife.inject(this, view);

        bg.setYAxis(yaxisView);
        refreshGraph();

        return view;
    }

    public void refreshGraphAsync() {
        getActivity().runOnUiThread(refreshGraphTask);
    }

    public void refreshGraph() {
        final ArrayList<Bar> points = addPoints();

        bg.setPadding(BAR_PADDING);
        bg.setBarSize(BAR_SIZE);
        bg.setBars(points);
        bg.setOnBarClickedListener(new BarGraph.OnBarClickedListener() {

            @DebugLog
            @Override
            public void onClick(int index) {
                onBarClick(index);
            }
        });

        bg.invalidate();
    }

    private ArrayList<Bar> addPoints() {
        final ArrayList<Bar> points = new ArrayList<Bar>();
        workoutUids = new SparseArray<String>();

        final File mainDir = getActivity().getExternalFilesDir(null);
        if (mainDir == null || !mainDir.isDirectory() || !mainDir.canRead()) {
            return points;
        }

        final File[] dirs = mainDir.listFiles();
        if (dirs == null || dirs.length == 0) {
            return points;
        }

        int i = 0;
        for (File dayDir : dirs) {
            if (!dayDir.canRead()) {
                continue;
            }

            final String uid = dayDir.getName();

            final IronMessage.WorkoutInfo workoutInfo;
            try {
                workoutInfo = FileUtils.fileToProtobuf(
                        new File(dayDir, AppConsts.WORKOUT_INFO_FILENAME));
            } catch (FileNotFoundException e) {
                Log.d(TAG, uid + " workout file not found.");
                continue;
            } catch (IOException e) {
                e.printStackTrace();
                continue;
            }

            int maxWeight = -1;
            for (IronMessage.Set set : workoutInfo.getSetList()) {
                int weight = set.getWeight();
                maxWeight = Math.max(maxWeight, weight);
            }

            final Bar d = new Bar();
            d.setColor(Color.BLUE);
            d.setValue(maxWeight);
            d.setName(new SimpleDateFormat("MMM. d").format(new Date(Long.parseLong(uid))));
            points.add(d);

            workoutUids.put(i++, uid);
        }

        return points;
    }

    private void onBarClick(int i) {
        final Intent intent = new Intent(getActivity(), ReviewPastActivity.class);
        intent.putExtra(ReviewPastActivity.UID, workoutUids.get(i));
        startActivity(intent);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        ButterKnife.reset(this);
    }
}
