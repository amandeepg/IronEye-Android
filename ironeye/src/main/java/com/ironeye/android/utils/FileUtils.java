package com.ironeye.android.utils;

import android.app.Activity;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

public class FileUtils {

    private static final int BYTE_BUFFER_SIZE = 4096;

    public static void inputStreamToFile(InputStream in, File vidFile) throws IOException {
        FileOutputStream fos = new FileOutputStream(vidFile);

        int read;
        byte[] bytes = new byte[BYTE_BUFFER_SIZE];
        while ((read = in.read(bytes)) != -1) {
            fos.write(bytes, 0, read);
        }

        fos.close();
    }

    public static File getDayFile(Activity act, String uid) {
        return new File(act.getExternalFilesDir(null), uid);
    }

    public static File getDayFile(Activity act, String uid, String fileName) {
        return new File(act.getExternalFilesDir(null), uid + File.separator + fileName);
    }
}
