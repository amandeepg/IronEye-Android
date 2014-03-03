package com.ironeye.android;

import android.app.Fragment;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import butterknife.ButterKnife;
import butterknife.InjectView;

import static com.ironeye.android.ControlsFragment.ControlPage.Adjacents;

public final class ControlsFragment extends Fragment {

    @InjectView(R.id.status_text)
    TextView statusText;

    @InjectView(R.id.left_text)
    TextView leftText;

    @InjectView(R.id.right_text)
    TextView rightText;

    private Adjacents mAdjacents;

    public static ControlsFragment newInstance(Adjacents adjacents) {
        ControlsFragment fragment = new ControlsFragment();

        fragment.mAdjacents = adjacents;

        return fragment;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        final View view = inflater.inflate(R.layout.single_control, container, false);
        ButterKnife.inject(this, view);

        ControlPage left = mAdjacents.left;
        ControlPage right = mAdjacents.right;
        ControlPage current = mAdjacents.current;

        statusText.setText(current.presentTenseAction);
        if (left != null) {
            leftText.setVisibility(View.VISIBLE);
            leftText.setText(left.futureTenseAction.replace(" ", "\n"));
        }
        if (right != null) {
            rightText.setVisibility(View.VISIBLE);
            rightText.setText(right.futureTenseAction.replace(" ", "\n"));
        }

        view.setBackgroundColor(current.colour);

        return view;
    }

    public static class ControlPage {
        public int colour;
        public String presentTenseAction;
        public String futureTenseAction;

        public ControlPage(int colour, String presentTenseAction, String futureTenseAction) {
            this.colour = colour;
            this.presentTenseAction = presentTenseAction;
            this.futureTenseAction = futureTenseAction;
        }

        public static class Adjacents {
            public ControlPage left;
            public ControlPage current;
            public ControlPage right;

            public Adjacents(ControlPage left, ControlPage current, ControlPage right) {
                this.left = left;
                this.current = current;
                this.right = right;
            }
        }
    }
}