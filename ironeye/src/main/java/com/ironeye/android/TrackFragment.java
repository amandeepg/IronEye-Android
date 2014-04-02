package com.ironeye.android;

import android.app.Activity;
import android.app.Fragment;
import android.content.Context;
import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.os.Vibrator;
import android.support.v4.view.ViewPager;
import android.text.InputFilter;
import android.view.GestureDetector;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.SimpleAdapter;
import android.widget.TextView;
import android.widget.Toast;

import com.android.colorpicker.ColorPickerDialog;
import com.android.colorpicker.ColorPickerSwatch;
import com.android.colorpicker.ColorStateDrawable;
import com.ironeye.IronEyeProtos;
import com.ironeye.android.utils.FileUtils;
import com.ironeye.android.utils.MinMaxInputFilter;
import com.mattyork.colours.Colour;
import com.nhaarman.listviewanimations.swinginadapters.AnimationAdapter;
import com.nhaarman.listviewanimations.swinginadapters.prepared.SwingRightInAnimationAdapter;
import com.viewpagerindicator.CirclePageIndicator;

import java.util.ArrayList;
import java.util.Map;

import butterknife.ButterKnife;
import butterknife.InjectView;
import butterknife.OnClick;
import hugo.weaving.DebugLog;

import static com.ironeye.IronEyeProtos.IronMessage;
import static com.ironeye.IronEyeProtos.IronMessage.WorkoutInfo;

public class TrackFragment extends Fragment {

    public static final int REAL_TIME_TYPE = 1;
    public static final int HISTORICAL_TYPE = 2;
    private static final String[] FROM = new String[]{"name", "msg"};
    private static final int[] TO = new int[]{android.R.id.text1, android.R.id.text2};
    private final int type;
    public WorkoutInfo workoutInfo;
    private int mCurrTargetReps = -1;

    @InjectView(R.id.listView)
    ListView lv;

    @InjectView(R.id.user_input_lay)
    ViewGroup userInputLay;

    @InjectView(R.id.sets_info_holder)
    ViewGroup setsInfoHolder;

    @InjectView(R.id.weight_entry_holder)
    ViewGroup weightEntryHolder;

    @InjectView(R.id.workout_info_holder)
    ViewGroup workoutInfoHolder;

    @InjectView(R.id.play_video_button)
    Button playVideoBut;

    @InjectView(R.id.controls)
    ViewGroup controlsLay;

    @InjectView(R.id.pager)
    StoppableViewPager mPager;

    @InjectView(R.id.indicator)
    CirclePageIndicator mIndicator;

    @InjectView(R.id.weight_edit)
    EditText weightEditText;

    @InjectView(R.id.reps_edit)
    EditText repsEditText;

    @InjectView(R.id.colourButton)
    ImageView colourButton;

    private ArrayList<Map<String, String>> mLst;
    private SimpleAdapter mListAdapter;
    private AnimationAdapter mAnimationAdapter;
    private String uid;
    private ControlsFragmentAdapter mViewPagerAdapter;
    private boolean exerciseAlreadyStarted;
    private boolean setControlFromServer = true;
    private int[] cols;
    private int selectedColour;

    public TrackFragment(int type) {
        this.type = type;
    }

    @DebugLog
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        final View view = inflater.inflate(R.layout.real_time_fragment, container, false);
        ButterKnife.inject(this, view);

        if (type == REAL_TIME_TYPE) {
            workoutInfoHolder.setVisibility(View.GONE);
            controlsLay.setVisibility(View.VISIBLE);
            weightEntryHolder.setVisibility(View.VISIBLE);
            lv.setVisibility(View.VISIBLE);
            userInputLay.setVisibility(View.VISIBLE);
            playVideoBut.setEnabled(false);

            setUpListView();
            setUpViewPager();

            if (exerciseAlreadyStarted) {
                onExerciseStarted();
            } else {
                onExerciseOver();
            }
        } else if (type == HISTORICAL_TYPE) {
            workoutInfoHolder.setVisibility(View.VISIBLE);
            controlsLay.setVisibility(View.GONE);
            weightEntryHolder.setVisibility(View.GONE);
            lv.setVisibility(View.GONE);
            userInputLay.setVisibility(View.GONE);
            playVideoBut.setEnabled(true);

            displayWorkoutInfo(workoutInfo);
        }

        weightEditText.setFilters(new InputFilter[]{ new MinMaxInputFilter(0, 999)});
        repsEditText.setFilters(new InputFilter[]{ new MinMaxInputFilter(0, 99)});

        final String[] colsStr = getResources().getStringArray(R.array.default_color_choice_values);
        cols = new int[colsStr.length];
        for (int i = 0; i < colsStr.length; i++) {
            cols[i] = Colour.parseColor(colsStr[i]);
        }
        setSelectedColour(cols[6]);

        return view;
    }

    private void setSelectedColour(int col) {
        selectedColour = col;
        colourButton.setImageDrawable(new ColorStateDrawable(new Drawable[]
                {getResources().getDrawable(R.drawable.color_picker_swatch)}, selectedColour));
    }

    private void setUpListView() {
        mLst = new ArrayList<Map<String, String>>();
        mListAdapter = new SimpleAdapter(getActivity(), mLst, R.layout.card_two_item, FROM, TO);

        mAnimationAdapter = new SwingRightInAnimationAdapter(mListAdapter);
        mAnimationAdapter.setAbsListView(lv);
        lv.setAdapter(mAnimationAdapter);
    }

    private void setUpViewPager() {
        mViewPagerAdapter = new ControlsFragmentAdapter(
                getActivity().getFragmentManager(), getActivity().getApplicationContext());
        mPager.setAdapter(mViewPagerAdapter);
        setUpPagerTouchListener();

        mIndicator.setViewPager(mPager);
        mIndicator.setOnPageChangeListener(new ViewPager.SimpleOnPageChangeListener() {
            @Override
            public void onPageSelected(int position) {
                TrackFragment.this.onPageSelected(position);
            }
        });
    }

    private void setUpPagerTouchListener() {
        final GestureDetector gd = new GestureDetector(getActivity(), new GestureDetector.SimpleOnGestureListener() {
            @Override
            public boolean onSingleTapUp(MotionEvent e) {
                return onTapViewPager(e);
            }
        });
        mPager.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                return gd.onTouchEvent(event);
            }
        });
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
        lv.requestFocus();
        mListAdapter.notifyDataSetChanged();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        ButterKnife.reset(this);
    }

    public void displayWorkoutInfo(final WorkoutInfo workoutInfo) {
        int i = 1;
        for (IronMessage.Set set : workoutInfo.getSetList()) {
            final View view = getActivity().getLayoutInflater().inflate(R.layout.rep_weight_card, setsInfoHolder, false);

            final TextView repView = ButterKnife.findById(view, R.id.rep_count);
            final TextView weightView = ButterKnife.findById(view, R.id.weight_for_set);
            final TextView setNumView = ButterKnife.findById(view, R.id.set_num);

            repView.setText(String.valueOf(set.getReps()));
            weightView.setText(String.valueOf(set.getWeight()));
            setNumView.setText("Set " + i++);

            setsInfoHolder.addView(view);
        }

        if (type == REAL_TIME_TYPE) {
            workoutInfoHolder.setVisibility(View.VISIBLE);
            final Animation slide = AnimationUtils.loadAnimation(getActivity().getApplicationContext(), R.anim.slide_up);
            workoutInfoHolder.startAnimation(slide);
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
        final Intent intent = new Intent(Intent.ACTION_VIEW);
        intent.setDataAndType(Uri.fromFile(FileUtils.getDayFile(getActivity(), uid, AppConsts.VIDEO_FILENAME)), "video/*");
        startActivity(intent);
    }

    @OnClick(R.id.colourButton)
    public void clickColourChange() {

        final ColorPickerDialog colorPicker = ColorPickerDialog.newInstance(
                R.string.color_picker_default_title,
                cols,
                selectedColour,
                4,
                ColorPickerDialog.SIZE_SMALL);

        colorPicker.setOnColorSelectedListener(new ColorPickerSwatch.OnColorSelectedListener() {

            @Override
            public void onColorSelected(int colour) {
                setSelectedColour(colour);
                sendDotColourMessage();
            }

        });
        colorPicker.show(getFragmentManager(), "colourPicker");
    }

    public void onExerciseOver() {
        mPager.setEnabled(false);
        mPager.setCurrentItem(2);
        mIndicator.setVisibility(View.INVISIBLE);
    }

    public void onExerciseStarted() {
        // if fragment not yet created, delay to when created
        if (mPager == null) {
            exerciseAlreadyStarted = true;
            return;
        }

        mPager.setEnabled(true);
        mPager.setCurrentItem(1, false);
        mIndicator.setVisibility(View.VISIBLE);
        mViewPagerAdapter.resetSetCount();

        workoutInfoHolder.setVisibility(View.GONE);
        setsInfoHolder.removeAllViews();
    }

    public boolean onTapViewPager(MotionEvent ev) {
        final int changePos = (ev.getX() < mPager.getWidth() / 2) ? -1 : 1;
        final int newPos = mPager.getCurrentItem() + changePos;

        if (newPos == 0) {
            try {
                getEnteredWeight();
            } catch (NumberFormatException ex) {
                promptWeightToast();
                return true;
            }
        }

        mPager.setCurrentItem(mPager.getCurrentItem() + changePos, true);
        return true;
    }

    private int getEnteredWeight() {
        return Integer.parseInt(weightEditText.getText().toString());
    }

    private int getEnteredReps() {
        try {
            return Integer.parseInt(repsEditText.getText().toString());
        } catch (NumberFormatException e) {
            return -1;
        }
    }

    @DebugLog
    public void setCurrentControlItem(int i) {
        mPager.setCurrentItem(i, true);
    }

    @DebugLog
    private void onPageSelected(int position) {
        final MainActivity act = (MainActivity) getActivity();

        if (!setControlFromServer) {
            IronMessage.MessageType msgType = null;
            switch (position) {
                case 0:
                    try {
                        final int weight = getEnteredWeight();
                        final int reps = getEnteredReps();
                        if (reps > 0) {
                            mCurrTargetReps = reps;
                        } else {
                            mCurrTargetReps = -1;
                        }
                        act.serverComms.sendMsgAsync(IronMessage.newBuilder()
                                .setType(IronMessage.MessageType.SET_START)
                                .setSet(IronMessage.Set.newBuilder()
                                        .setWeight(weight)
                                        .setReps(reps)));
                        sendDotColourMessage();
                    } catch (NumberFormatException e) {
                        promptWeightToast();
                        mPager.setCurrentItem(position + 1, true);
                    }
                    break;
                case 1:
                    msgType = IronMessage.MessageType.SET_END;
                    break;
                case 2:
                    msgType = IronMessage.MessageType.EXERCISE_END;
                    break;
            }
            act.serverComms.sendControlMsgAsync(msgType);
        }
        setControlFromServer = false;

        switch (position) {
            case 0:
                mViewPagerAdapter.incrementSetCount();
                break;
            case 2:
                onExerciseOver();
                break;
        }
    }

    private void sendDotColourMessage() {
        ((MainActivity) getActivity())
                .serverComms.sendMsgAsync(IronMessage.newBuilder()
                .setType(IronMessage.MessageType.CHANGE_DOT_COLOUR)
                .setDotColour(selectedColour));
    }

    private void promptWeightToast() {
        final Activity activity = getActivity();
        final Vibrator vib = (Vibrator) activity.getSystemService(Context.VIBRATOR_SERVICE);

        Toast.makeText(activity, activity.getString(R.string.please_enter_weight), Toast.LENGTH_SHORT).show();
        vib.vibrate(200);
    }

    @DebugLog
    public void setSetControlFromServer(boolean setControlFromServer) {
        this.setControlFromServer = setControlFromServer;
    }

    public void onRepCompleted(int rep) {
        if (rep == mCurrTargetReps) {
            mPager.setCurrentItem(1, true);
        }
    }
}
