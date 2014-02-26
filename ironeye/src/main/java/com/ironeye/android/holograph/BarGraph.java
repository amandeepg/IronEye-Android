/*
 *     Created by Daniel Nadeau
 *     daniel.nadeau01@gmail.com
 *     danielnadeau.blogspot.com
 * 
 *     Licensed to the Apache Software Foundation (ASF) under one
       or more contributor license agreements.  See the NOTICE file
       distributed with this work for additional information
       regarding copyright ownership.  The ASF licenses this file
       to you under the Apache License, Version 2.0 (the
       "License"); you may not use this file except in compliance
       with the License.  You may obtain a copy of the License at

         http://www.apache.org/licenses/LICENSE-2.0

       Unless required by applicable law or agreed to in writing,
       software distributed under the License is distributed on an
       "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
       KIND, either express or implied.  See the License for the
       specific language governing permissions and limitations
       under the License.
 */

package com.ironeye.android.holograph;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;

import java.util.ArrayList;

public class BarGraph extends View {

    public static final int HIGHLIGHT_COLOUR = Color.parseColor("#33B5E5");
    private final static int AXIS_LABEL_FONT_SIZE = 15;
    private ArrayList<Bar> mBars = new ArrayList<Bar>();
    private boolean mShowAxis = true;
    private int mIndexSelected = -1;
    private OnBarClickedListener mListener;
    private float mPadding = 7;
    private int mBarSize = -1;

    private Paint mPaint = new Paint();
    private RectF mRectClip = new RectF();
    private RectF mRectF = new RectF();

    private Context mContext = null;

    public BarGraph(Context context) {
        super(context);
        mContext = context;
    }

    public BarGraph(Context context, AttributeSet attrs) {
        super(context, attrs);
        mContext = context;
    }

    public void setShowAxis(boolean show) {
        mShowAxis = show;
    }

    public void setPadding(float padding) {
        mPadding = padding;
    }

    public void setBarSize(int barSize) {
        mBarSize = barSize;
    }

    public ArrayList<Bar> getBars() {
        return this.mBars;
    }

    public void setBars(ArrayList<Bar> points) {
        this.mBars = points;
        postInvalidate();
    }

    public void onDraw(Canvas canvas) {
        canvas.drawColor(Color.TRANSPARENT);
        mPaint.setAntiAlias(true);

        float maxValue = 0;
        float density = mContext.getResources().getDisplayMetrics().density;
        float scaledDensity = mContext.getResources().getDisplayMetrics().scaledDensity;
        float padding = mPadding * density;
        float bottomPadding = 30 * density;
        float strokeWidth = padding + 0;
        float roundRadius = padding * 5f;
        float usableHeight = getHeight() - bottomPadding - 5;

        // Draw x-axis line
        if (mShowAxis) {
            mPaint.setColor(Color.BLACK);
            mPaint.setStrokeWidth(2 * density);
            mPaint.setAlpha(50);
            canvas.drawLine(0, getHeight() - bottomPadding + 10 * density, getWidth(), getHeight() - bottomPadding + 10 * density, mPaint);
        }
        float barWidth = (getWidth() - (padding * 2) * mBars.size()) / mBars.size();

        // Maximum y value = sum of all values.
        for (final Bar bar : mBars) {
            if (bar.getValue() > maxValue) {
                maxValue = bar.getValue();
            }
        }

        int count = 0;
        for (final Bar bar : mBars) {
            // Set bar bounds
            float left = (padding * 2f) * count + padding + barWidth * count;
            float top = getHeight() - bottomPadding - (usableHeight * (bar.getValue() / maxValue));
            float right = (padding * 2f) * count + padding + barWidth * (count + 1);
            float bottom = getHeight() - bottomPadding;
            mRectF.set(left, top, right, bottom);
            bar.setRect(mRectF);

            canvas.save();
            mRectClip.set(mRectF);
            mRectClip.inset(-padding * 2f, -padding * 2f);
            mRectClip.offset(0, -mRectClip.height() / 2f + 1);
            canvas.clipRect(mRectClip);
            drawOutlinedRoundRect(canvas, mRectF, bar.getColor(), Color.BLACK, strokeWidth, roundRadius);
            canvas.restore();

            canvas.save();
            mRectClip.set(mRectF);
            mRectClip.inset(-padding * 2f, -padding * 2f);
            mRectClip.offset(0, mRectClip.height() / 2f - 1);
            canvas.clipRect(mRectClip);
            drawOutlinedRoundRect(canvas, mRectF, bar.getColor(), Color.BLACK, strokeWidth, 0);
            canvas.restore();

            // Draw x-axis label text
            if (mShowAxis) {
                mPaint.setColor(bar.getColor());

                int fontModifier = 0;
                do {
                    mPaint.setTextSize((AXIS_LABEL_FONT_SIZE - fontModifier) * scaledDensity);
                    fontModifier -= 0.1;
                } while (mPaint.measureText(bar.getName()) < barWidth * 0.85);
                float x = mRectF.centerX() - (mPaint.measureText(bar.getName()) / 2f);
                float y = getHeight() - 3 * scaledDensity;
                canvas.drawText(bar.getName(), x, y, mPaint);
            }

            if (mIndexSelected == count && mListener != null) {
                mPaint.setColor(HIGHLIGHT_COLOUR);
                mPaint.setAlpha(100);
                mRectF.inset(-padding * 2f, -padding * 2f);
                canvas.drawRect(mRectF, mPaint);
                mPaint.setAlpha(255);
            }
            count++;
        }
    }

    private void drawOutlinedRoundRect(Canvas canvas, RectF rectF, int fillColour, int strokeColour, float strokeWidth, float roundRadius) {
        mPaint.setColor(fillColour);
        canvas.drawRoundRect(rectF, roundRadius, roundRadius, mPaint);

        mPaint.setColor(strokeColour);
        mPaint.setStyle(Paint.Style.STROKE);
        mPaint.setStrokeWidth(strokeWidth);
        canvas.drawRoundRect(rectF, roundRadius, roundRadius, mPaint);
        mPaint.setStyle(Paint.Style.FILL);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        float x = event.getX();
        float y = event.getY();

        int count = 0;
        for (Bar bar : mBars) {
            if (event.getAction() == MotionEvent.ACTION_DOWN && bar.getRect().contains(x, y)) {
                mIndexSelected = count;
            } else if (event.getAction() == MotionEvent.ACTION_UP &&
                    bar.getRect().contains(x, y) && mListener != null && mIndexSelected > -1) {
                mListener.onClick(mIndexSelected);
                mIndexSelected = -1;
            } else if (event.getAction() == MotionEvent.ACTION_CANCEL) {
                mIndexSelected = -1;
            }

            count++;
        }

        if (event.getAction() == MotionEvent.ACTION_DOWN ||
                event.getAction() == MotionEvent.ACTION_UP ||
                event.getAction() == MotionEvent.ACTION_CANCEL) {
            postInvalidate();
        }

        return true;
    }

    public void setOnBarClickedListener(OnBarClickedListener listener) {
        this.mListener = listener;
    }

    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int widthMode = MeasureSpec.getMode(widthMeasureSpec);
        int widthSize = MeasureSpec.getSize(widthMeasureSpec);

        int desiredWidth;
        if (mBarSize != -1) {
            desiredWidth = (int) ((mBarSize + 2f * mPadding) * mBars.size());
        } else {
            desiredWidth = widthSize;
        }

        int width;
        int height = MeasureSpec.getSize(heightMeasureSpec);

        if (widthMode == MeasureSpec.EXACTLY) {
            width = widthSize;
        } else if (widthMode == MeasureSpec.AT_MOST) {
            width = Math.min(desiredWidth, widthSize);
        } else {
            width = desiredWidth;
        }

        setMeasuredDimension(width, height);
    }

    public interface OnBarClickedListener {
        abstract void onClick(int index);
    }
}
