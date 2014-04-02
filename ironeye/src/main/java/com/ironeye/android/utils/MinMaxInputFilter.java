package com.ironeye.android.utils;

import android.text.InputFilter;
import android.text.Spanned;

public class MinMaxInputFilter implements InputFilter {

    private final int mMin, mMax;

    public MinMaxInputFilter(int min, int max) {
        mMin = min;
        mMax = max;
    }

    @Override
    public CharSequence filter(CharSequence source, int start, int end, Spanned dest, int dstart, int dend) {
        try {
            final int input = Integer.parseInt(dest.toString() + source.toString());

            if (isInRange(mMin, mMax, input)) {
                return null;
            } else {
                return "";
            }
        } catch (NumberFormatException e) {
            return "";
        }
    }

    private boolean isInRange(int start, int end, int i) {
        return end > start ? i >= start && i <= end : i >= end && i <= start;
    }

}
