package com.ironeye.android;

import android.app.Fragment;
import android.app.FragmentManager;
import android.support.v13.app.FragmentPagerAdapter;

import com.mattyork.colours.Colour;

import static com.ironeye.android.ControlsFragment.ControlPage;
import static com.ironeye.android.ControlsFragment.ControlPage.Adjacents;

class ControlsFragmentAdapter extends FragmentPagerAdapter {

    private final int COUNT = 3;

    private int set_num;
    private ControlsFragment setProgressControlPage;

    public ControlsFragmentAdapter(FragmentManager fm) {
        super(fm);

        resetSetCount();
    }

    @Override
    public Fragment getItem(int position) {
        ControlPage[] controlPages = new ControlPage[]{
                new ControlPage(Colour.successColor(), "On Set " + set_num, "Start Set"),
                new ControlPage(Colour.bananaColor(), "Paused", "Pause"),
                new ControlPage(Colour.watermelonColor(), "Exercise Ended", "End Exercise"),
        };

        Adjacents adj = new Adjacents(
                position - 1 >= 0 && position - 1 < COUNT - 2 ? controlPages[position - 1] : null,
                controlPages[position],
                position + 1 < COUNT ? controlPages[position + 1] : null
        );
        ControlsFragment frag = ControlsFragment.newInstance(adj);
        if (position == 0) {
            setProgressControlPage = frag;
        }

        return frag;
    }

    @Override
    public int getCount() {
        return COUNT;
    }

    public void resetSetCount() {
        set_num = 0;
    }

    public void incrementSetCount() {
        set_num++;
        if (setProgressControlPage != null) {
            setProgressControlPage.statusText.setText("On Set " + set_num);
        }
    }
}