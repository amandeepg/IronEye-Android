package com.ironeye.android;

import android.app.Fragment;
import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.ironeye.android.holograph.Bar;
import com.ironeye.android.holograph.BarGraph;

import java.util.ArrayList;
import java.util.Random;

import hugo.weaving.DebugLog;

public class LogFragment extends Fragment {

    public LogFragment() {
    }

    /**
     * Returns a new instance of this fragment for the given section
     * number.
     */
    public static LogFragment newInstance() {
        LogFragment fragment = new LogFragment();
        return fragment;
    }

    @DebugLog
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.log_fragment, container, false);

        Random rnd = new Random();

        ArrayList<Bar> points = new ArrayList<Bar>();
        for (int i = 1; i <= 31; i++) {
            Bar d = new Bar();
            float rndFloat = rnd.nextFloat();
            int val = (int) (50 + 30 * rndFloat);
            d.setColor(Color.rgb((int) (rndFloat * 74), (int) (rndFloat * 131), (int) (rndFloat * 255)));
            d.setValue(val);
            d.setName("Jan. " + i);
            points.add(d);
        }

        BarGraph g = (BarGraph) rootView.findViewById(R.id.graph);
        g.setPadding(1.5f);
        g.setBars(points);
        g.setBarSize(150);

        g.setOnBarClickedListener(new BarGraph.OnBarClickedListener() {

            @DebugLog
            @Override
            public void onClick(int index) {

            }
        });

        g.invalidate();

        return rootView;
    }
}
