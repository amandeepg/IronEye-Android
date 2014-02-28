package com.ironeye.android;

import android.app.Fragment;
import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.ironeye.android.holograph.Bar;
import com.ironeye.android.holograph.BarGraph;
import com.ironeye.android.holograph.YAxisView;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;

import butterknife.ButterKnife;
import butterknife.InjectView;
import hugo.weaving.DebugLog;

import static com.ironeye.IronEyeProtos.IronMessage;

public class LogFragment extends Fragment {

    public static final float BAR_PADDING = 1.5f;
    public static final int BAR_SIZE = 150;
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
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
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
        ArrayList<Bar> points = addPoints();

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
        ArrayList<Bar> points = new ArrayList<Bar>();
        File mainDir = getActivity().getExternalFilesDir(null);
        if (mainDir == null || !mainDir.isDirectory() || !mainDir.canRead()) {
            return points;
        }

        File[] dirs = mainDir.listFiles();
        if (dirs == null || dirs.length == 0) {
            return points;
        }

        for (File dayDir : dirs) {
            if (!dayDir.canRead()) {
                continue;
            }

            IronMessage.WorkoutInfo workoutInfo;
            try {
                workoutInfo = IronMessage.WorkoutInfo.parseFrom(
                        new FileInputStream(new File(dayDir, AppConsts.WORKOUT_INFO_FILENAME)));
            } catch (IOException e) {
                e.printStackTrace();
                continue;
            }

            int maxWeight = -1;
            for (IronMessage.Set set : workoutInfo.getSetList()) {
                int weight = set.getWeight();
                maxWeight = Math.max(maxWeight, weight);
            }

            Bar d = new Bar();
            d.setColor(Color.BLUE);
            d.setValue(maxWeight);
            d.setName(new SimpleDateFormat("MMM. d").format(new Date(Long.parseLong(dayDir.getName()))));
            points.add(d);
        }

        return points;
    }

    private void onBarClick(@SuppressWarnings("UnusedParameters") int i) {

    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        ButterKnife.reset(this);
    }
}
