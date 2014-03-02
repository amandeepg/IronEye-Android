package com.ironeye.android;

import android.content.Context;
import android.os.Vibrator;
import android.speech.tts.TextToSpeech;
import android.util.Log;

import com.ironeye.IronEyeProtos;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class ServerCommThread extends Thread {

    public static final int SERVER_PORT = 38300;
    private static final String TAG = "ServerCommThread";

    private Socket mSocket;
    private MainActivity mAct;

    public ServerCommThread(MainActivity mainAct) {
        super(TAG);
        this.mAct = mainAct;
    }

    @Override
    public void run() {
        try {
            receiveFromServer();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void receiveFromServer() throws IOException {
        ServerSocket mServerSocket = new ServerSocket(SERVER_PORT);
        mSocket = mServerSocket.accept();

        mAct.onExerciseStarted();

        IronEyeProtos.IronMessage.UserInfo.Builder userInfo = IronEyeProtos.IronMessage.UserInfo.newBuilder()
                .setId(AppController.getInstance().currentPerson.getId());

        IronEyeProtos.IronMessage msg = IronEyeProtos.IronMessage.newBuilder()
                .setUserInfo(userInfo)
                .setType(IronEyeProtos.IronMessage.MessageType.USER_INFO)
                .build();

        OutputStream out = mSocket.getOutputStream();
        InputStream in = mSocket.getInputStream();

        msg.writeDelimitedTo(out);
        String uid = String.valueOf(System.currentTimeMillis());

        boolean mkdirs = FileUtils.getDayFile(mAct, uid).mkdirs();
        if (!mkdirs) {
            Log.d(TAG, "Directory not made.");
        }

        final IronEyeProtos.IronMessage.WorkoutInfo workoutInfo;

        while (true) {
            IronEyeProtos.IronMessage statusMsg = IronEyeProtos.IronMessage.parseDelimitedFrom(in);
            if (statusMsg.getType().equals(IronEyeProtos.IronMessage.MessageType.WORKOUT_INFO)) {
                workoutInfo = statusMsg.getWorkoutInfo();
                break;
            }

            ArrayList<Map<String, String>> jointListData = new ArrayList<Map<String, String>>();
            boolean shouldVibrate = false;

            IronEyeProtos.IronMessage.FormErrorData formError = statusMsg.getErrorData();
            for (IronEyeProtos.IronMessage.JointError je : formError.getJointList()) {
                String jointType = je.getJointType().toString();
                String jointMsg = je.getErrorMessage();

                HashMap<String, String> item = new HashMap<String, String>();
                item.put("name", jointType);
                item.put("msg", jointMsg);
                jointListData.add(item);

                mAct.tts.speak(jointType.replace("_", " "), TextToSpeech.QUEUE_ADD, null);
                shouldVibrate = true;
            }
            if (shouldVibrate) {
                Vibrator vib = (Vibrator) mAct.getSystemService(Context.VIBRATOR_SERVICE);
                vib.vibrate(200);
            }

            mAct.refreshTrackingList(jointListData);
        }

        final FileOutputStream fos = new FileOutputStream(FileUtils.getDayFile(mAct, uid, AppConsts.WORKOUT_INFO_FILENAME));
        workoutInfo.writeTo(fos);
        fos.close();

        mAct.refreshTrackingList(new ArrayList<Map<String, String>>());
        mAct.addWorkoutInfoToTrackingFrag(workoutInfo);
        mAct.refreshLogGraph();

        final File vidFile = FileUtils.getDayFile(mAct, uid, AppConsts.VIDEO_FILENAME);
        FileUtils.inputStreamToFile(in, vidFile);

        mAct.videoReady(uid);

        mSocket.close();
        mServerSocket.close();

        mAct.startServerSocket();
    }

    public void sendControlMsg(final IronEyeProtos.IronMessage.MessageType type) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                IronEyeProtos.IronMessage msg = IronEyeProtos.IronMessage.newBuilder()
                        .setType(type)
                        .build();

                try {
                    msg.writeDelimitedTo(mSocket.getOutputStream());
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }).start();
    }
}
