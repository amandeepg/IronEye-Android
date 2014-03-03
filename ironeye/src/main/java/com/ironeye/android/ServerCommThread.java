package com.ironeye.android;

import android.content.Context;
import android.os.Vibrator;
import android.speech.tts.TextToSpeech;
import android.util.Log;

import com.ironeye.android.utils.FileUtils;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.ref.WeakReference;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import hugo.weaving.DebugLog;

import static com.ironeye.IronEyeProtos.IronMessage;

public class ServerCommThread extends Thread {

    private static final String TAG = "ServerCommThread";

    private Socket mSocket;
    private WeakReference<MainActivity> mAct;
    private String mUid;

    public ServerCommThread(MainActivity mainAct) {
        super(TAG);
        this.mAct = new WeakReference<MainActivity>(mainAct);
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
        final ServerSocket mServerSocket = new ServerSocket(AppConsts.SERVER_PORT);
        mSocket = mServerSocket.accept();
        final InputStream in = mSocket.getInputStream();

        getAct().onExerciseStarted();

        sendUserMsg();

        final boolean mkdirs = FileUtils.getDayFile(getAct(), getUid()).mkdirs();
        if (!mkdirs) {
            Log.d(TAG, "Directory not made.");
        }

        boolean isWorkoutMsg = false;
        while (!isWorkoutMsg) {
            IronMessage statusMsg = IronMessage.parseDelimitedFrom(in);
            IronMessage.MessageType type = statusMsg.getType();
            switch (type) {
                case WORKOUT_INFO:
                    handleWorkoutInfoMsg(statusMsg);
                    isWorkoutMsg = true;
                    break;
                case FORM_ERROR:
                    handleJointErrorMsg(statusMsg);
                    break;
                case SET_START:
                case SET_END:
                case EXERCISE_END:
                    getAct().moveControls(type);
                    break;
            }
        }

        handleVideoStream();

        mSocket.close();
        mServerSocket.close();

        getAct().startServerSocket();
    }

    @DebugLog
    private void handleVideoStream() throws IOException {
        final File vidFile = FileUtils.getDayFile(getAct(), getUid(), AppConsts.VIDEO_FILENAME);
        FileUtils.inputStreamToFile(mSocket.getInputStream(), vidFile);

        getAct().videoReady(getUid());
    }

    private String getUid() {
        if (mUid == null) {
            mUid = String.valueOf(System.currentTimeMillis());
        }
        return mUid;
    }

    @DebugLog
    private void handleWorkoutInfoMsg(IronMessage statusMsg) throws IOException {
        final IronMessage.WorkoutInfo workoutInfo = statusMsg.getWorkoutInfo();

        final FileOutputStream fos = new FileOutputStream(FileUtils.getDayFile(getAct(), getUid(), AppConsts.WORKOUT_INFO_FILENAME));
        workoutInfo.writeTo(fos);
        fos.close();

        getAct().refreshTrackingList(new ArrayList<Map<String, String>>());
        getAct().addWorkoutInfoToTrackingFrag(workoutInfo);
        getAct().refreshLogGraph();
    }

    @DebugLog
    private void handleJointErrorMsg(IronMessage statusMsg) {
        final ArrayList<Map<String, String>> jointListData = new ArrayList<Map<String, String>>();

        final IronMessage.FormErrorData formError = statusMsg.getErrorData();

        boolean shouldVibrate = false;

        for (IronMessage.JointError je : formError.getJointList()) {
            final String jointType = je.getJointType().toString();
            final String jointMsg = je.getErrorMessage();

            final HashMap<String, String> item = new HashMap<String, String>();
            item.put("name", jointType);
            item.put("msg", jointMsg);
            jointListData.add(item);

            getAct().tts.speak(jointType.replace("_", " "), TextToSpeech.QUEUE_ADD, null);
            shouldVibrate = true;
        }
        if (shouldVibrate) {
            Vibrator vib = (Vibrator) getAct().getSystemService(Context.VIBRATOR_SERVICE);
            vib.vibrate(200);
        }

        getAct().refreshTrackingList(jointListData);
    }

    private void sendUserMsg() throws IOException {
        final IronMessage.UserInfo.Builder userInfo = IronMessage.UserInfo.newBuilder()
                .setId(AppController.getInstance().currentPerson.getId());

        final IronMessage.Builder userMsg = IronMessage.newBuilder()
                .setUserInfo(userInfo)
                .setType(IronMessage.MessageType.USER_INFO);

        sendMsg(userMsg);
    }

    @DebugLog
    private synchronized void sendMsg(IronMessage msg) throws IOException {
        msg.writeDelimitedTo(mSocket.getOutputStream());
    }

    private void sendMsg(IronMessage.Builder msg) throws IOException {
        sendMsg(msg.build());
    }

    public void sendMsgAsync(final IronMessage.Builder msg) {
        if (msg == null) {
            return;
        }
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    sendMsg(msg);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }).start();
    }

    public void sendControlMsgAsync(final IronMessage.MessageType type) {
        if (type == null) {
            return;
        }
        new Thread(new Runnable() {
            @Override
            public void run() {
                final IronMessage.Builder msg = IronMessage.newBuilder()
                        .setType(type);
                try {
                    sendMsg(msg);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }).start();
    }

    private MainActivity getAct() {
        return mAct.get();
    }
}
