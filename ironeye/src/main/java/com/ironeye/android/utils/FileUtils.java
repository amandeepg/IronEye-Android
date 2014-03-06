package com.ironeye.android.utils;

import com.ironeye.IronEyeProtos;

import android.app.Activity;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;

public class FileUtils {

    public static void byteArrayToFile(File vidFile, byte[] b) throws IOException {
        final FileOutputStream videoFileFos = new FileOutputStream(vidFile);
        videoFileFos.write(b);
        videoFileFos.close();
    }

    public static void protobufToFile(IronEyeProtos.IronMessage.WorkoutInfo workoutInfo, File f)
            throws IOException {
        final FileOutputStream workoutInfoFos = new FileOutputStream(f);
        workoutInfo.writeTo(workoutInfoFos);
        workoutInfoFos.close();
    }

    public static IronEyeProtos.IronMessage.WorkoutInfo fileToProtobuf(File f)
            throws IOException {
        return IronEyeProtos.IronMessage.WorkoutInfo.parseFrom(new FileInputStream(f));
    }

    public static File getDayFile(Activity act, String uid) {
        return new File(act.getExternalFilesDir(null), uid);
    }

    public static File getDayFile(Activity act, String uid, String fileName) {
        return new File(act.getExternalFilesDir(null), uid + File.separator + fileName);
    }
}
