package com.ironeye.android.holograph;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.view.View;

public class YAxisView extends View {
    public static final float FONT_SIZE = 14f;
    private static final int NUM_TICKS = 5;
    private Context mContext;
    private int max = -1;
    private Paint mPaint = new Paint();

    public YAxisView(Context context) {
        super(context);
        mContext = context;
    }

    public YAxisView(Context context, AttributeSet attrs) {
        super(context, attrs);
        mContext = context;
    }

    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        final int widthMode = MeasureSpec.getMode(widthMeasureSpec);
        final int widthSize = MeasureSpec.getSize(widthMeasureSpec);

        final int desiredWidth;

        if (max != -1) {
            float scaledDensity = mContext.getResources().getDisplayMetrics().scaledDensity;

            mPaint.setTextSize(FONT_SIZE * scaledDensity);
            desiredWidth = (int) (mPaint.measureText(String.valueOf(max)) * 1.4);
        } else {
            desiredWidth = widthSize;
        }

        final int width;
        final int height = MeasureSpec.getSize(heightMeasureSpec);

        if (widthMode == MeasureSpec.EXACTLY) {
            width = widthSize;
        } else if (widthMode == MeasureSpec.AT_MOST) {
            width = Math.min(desiredWidth, widthSize);
        } else {
            width = desiredWidth;
        }

        setMeasuredDimension(width, height);
    }

    public void onDraw(Canvas canvas) {
        canvas.drawColor(Color.TRANSPARENT);
        mPaint.setAntiAlias(true);

        final float density = mContext.getResources().getDisplayMetrics().density;
        final float scaledDensity = mContext.getResources().getDisplayMetrics().scaledDensity;
        final float bottomPadding = 30 * density;
        final float usableHeight = getHeight() - bottomPadding;
        final float width = getWidth();

        mPaint.setTextSize(FONT_SIZE * scaledDensity);

        mPaint.setStrokeWidth((int) (2f * density));
        canvas.drawLine(getWidth(), 0, width, usableHeight, mPaint);

        for (int k = 0; k <= NUM_TICKS; k++) {
            final float fraction = ((float) k / NUM_TICKS);
            final float lineHeightPos = usableHeight * fraction;
            canvas.drawLine(getWidth() * 0.75f, lineHeightPos, width, lineHeightPos, mPaint);

            final String s = String.valueOf((int) (max * (1 - fraction)));
            final float textHeightPos;

            if (k == 0) {
                textHeightPos = lineHeightPos + mPaint.getFontSpacing();
            } else if (k == NUM_TICKS) {
                textHeightPos = lineHeightPos;
            } else {
                textHeightPos = lineHeightPos + (mPaint.getFontSpacing() / 2f);
            }

            canvas.drawText(s,
                    getWidth() - mPaint.measureText(s) - (getWidth() * 0.3f) - 1,
                    textHeightPos - 3 * density,
                    mPaint);
        }
    }

    public void setMax(final int max) {
        this.max = max;
        postInvalidate();
    }
}
