package com.ironeye.android;

import android.app.Fragment;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Point;
import android.os.Bundle;
import android.util.Log;
import android.view.Display;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.WriterException;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel;

import java.util.Hashtable;

import hugo.weaving.DebugLog;

public class QrCodeFragment extends Fragment {

    private static final String TAG = "QrCodeFragment";

    public QrCodeFragment() {
    }

    /**
     * Returns a new instance of this fragment for the given section
     * number.
     */
    public static QrCodeFragment newInstance() {
        QrCodeFragment fragment = new QrCodeFragment();
        return fragment;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.qr_code_activity, container, false);

        ImageView imageView = (ImageView) rootView.findViewById(R.id.qrCodeImageView);

        Log.d(TAG, "AppController.getInstance().currentPerson = " +
                (AppController.getInstance().currentPerson != null));
        String content = AppController.getInstance().currentPerson.getId();
        Bitmap qrBmp = getQrCodeBitmap(content, getScreenWidth());
        imageView.setImageBitmap(qrBmp);

        return rootView;
    }

    @DebugLog
    private Bitmap getQrCodeBitmap(String content, int qrCodeSize) {
        Bitmap qrBmp = Bitmap.createBitmap(qrCodeSize, qrCodeSize, Bitmap.Config.ARGB_8888);

        Hashtable<EncodeHintType, ErrorCorrectionLevel> hintMap = new Hashtable<EncodeHintType, ErrorCorrectionLevel>();
        hintMap.put(EncodeHintType.ERROR_CORRECTION, ErrorCorrectionLevel.H);

        BitMatrix byteMatrix = null;
        try {
            byteMatrix = new QRCodeWriter().encode(content, BarcodeFormat.QR_CODE, qrCodeSize, qrCodeSize, hintMap);
        } catch (WriterException e) {
            e.printStackTrace();
        }

        int matrixWidth = byteMatrix.getWidth();

        Canvas canvas = new Canvas(qrBmp);
        Paint paint = new Paint();

        canvas.drawColor(Color.TRANSPARENT);

        paint.setColor(Color.BLACK);
        paint.setStyle(Paint.Style.FILL);

        for (int i = 0; i < matrixWidth; i++) {
            for (int j = 0; j < matrixWidth; j++) {
                if (byteMatrix.get(i, j)) {
                    canvas.drawPoint(i, j, paint);
                }
            }
        }
        return qrBmp;
    }

    @DebugLog
    private int getScreenWidth() {
        Display display = getActivity().getWindowManager().getDefaultDisplay();
        Point size = new Point();
        display.getSize(size);
        return size.x;
    }
}
