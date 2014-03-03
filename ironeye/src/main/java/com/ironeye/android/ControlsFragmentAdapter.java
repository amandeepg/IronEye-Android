package com.ironeye.android;

import android.app.Fragment;
import android.app.FragmentManager;
import android.content.Context;
import android.support.v13.app.FragmentPagerAdapter;

import com.mattyork.colours.Colour;

import static com.ironeye.android.ControlsFragment.ControlPage;
import static com.ironeye.android.ControlsFragment.ControlPage.Adjacents;

class ControlsFragmentAdapter extends FragmentPagerAdapter {

    private final int COUNT = 3;

    private int set_num;
    private ControlsFragment setProgressControlPage;
    private Context mContext;

    public ControlsFragmentAdapter(FragmentManager fm, Context context) {
        super(fm);

        mContext = context;
        resetSetCount();
    }

    @Override
    public Fragment getItem(int position) {
        final ControlPage[] controlPages = new ControlPage[]{
                new ControlPage(Colour.successColor(), mContext.getString(R.string.on_set_x, set_num), mContext.getString(R.string.start_set)),
                new ControlPage(Colour.bananaColor(), mContext.getString(R.string.paused), mContext.getString(R.string.end_set)),
                new ControlPage(Colour.watermelonColor(), mContext.getString(R.string.exercise_ended), mContext.getString(R.string.end_exercise)),
        };

        final Adjacents adj = new Adjacents(
                position - 1 >= 0 && position - 1 < COUNT - 2 ? controlPages[position - 1] : null,
                controlPages[position],
                position + 1 < COUNT ? controlPages[position + 1] : null
        );
        final ControlsFragment frag = ControlsFragment.newInstance(adj);
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
            setProgressControlPage.statusText.setText(mContext.getString(R.string.on_set_x, set_num));
        }
    }
}