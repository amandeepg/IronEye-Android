package com.ironeye.android;

import android.app.Fragment;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListView;
import android.widget.SimpleAdapter;

import com.nhaarman.listviewanimations.swinginadapters.AnimationAdapter;
import com.nhaarman.listviewanimations.swinginadapters.prepared.SwingRightInAnimationAdapter;

import java.util.ArrayList;
import java.util.Map;

import butterknife.ButterKnife;
import butterknife.InjectView;
import hugo.weaving.DebugLog;

public class TrackFragment extends Fragment {

    @InjectView(R.id.listView)
    ListView lv;

    private ArrayList<Map<String, String>> mLst;
    private SimpleAdapter mAdapter;
    private AnimationAdapter mAnimationAdapter;

    public TrackFragment() {
    }

    /**
     * Returns a new instance of this fragment for the given section
     * number.
     */
    public static TrackFragment newInstance() {
        return new TrackFragment();
    }

    @DebugLog
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        final View view = inflater.inflate(R.layout.real_time_fragment, container, false);
        ButterKnife.inject(this, view);

        mLst = new ArrayList<Map<String, String>>();
        final String[] from = {"name", "msg"};
        final int[] to = {android.R.id.text1, android.R.id.text2};
        mAdapter = new SimpleAdapter(getActivity(), mLst, R.layout.card_two_item, from, to);

        mAnimationAdapter = new SwingRightInAnimationAdapter(mAdapter);
        mAnimationAdapter.setAbsListView(lv);
        lv.setAdapter(mAnimationAdapter);

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
}
