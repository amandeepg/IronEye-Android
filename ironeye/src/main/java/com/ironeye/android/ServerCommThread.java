package com.ironeye.android;

import android.content.Context;
import android.os.Vibrator;
import android.speech.tts.TextToSpeech;
import android.util.Log;

import com.ironeye.android.utils.FileUtils;

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

    private ArrayList<String> prevErrors = new ArrayList<String>();

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

        boolean isCommOver = false;
        while (!isCommOver) {
            IronMessage statusMsg = IronMessage.parseDelimitedFrom(in);
            IronMessage.MessageType type = statusMsg.getType();
            switch (type) {
                case WORKOUT_INFO:
                    handleWorkoutInfoMsg(statusMsg);
                    break;
                case WORKOUT_UPDATE:
                    handleWorkoutUpdateMsg(statusMsg);
                    break;
                case VIDEO:
                    handleVideoMsg(statusMsg);
                    isCommOver = true;
                    break;
                case FORM_ERROR:
                    handleJointErrorMsg(statusMsg);
                    break;
                case SET_START:
                case SET_END:
                case EXERCISE_END:
                    getAct().moveControls(statusMsg);
                    break;
                default:
                    handleUnknown(statusMsg);
            }
        }

        mSocket.close();
        mServerSocket.close();

        getAct().startServerSocket();
    }

    @DebugLog
    private void handleUnknown(@SuppressWarnings("UnusedParameters") IronMessage statusMsg) {
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

        FileUtils.protobufToFile(workoutInfo,
                FileUtils.getDayFile(getAct(), getUid(), AppConsts.WORKOUT_INFO_FILENAME));

        getAct().refreshTrackingList(new ArrayList<Map<String, String>>());
        getAct().addWorkoutInfoToTrackingFrag(workoutInfo);
        getAct().refreshLogGraph();
    }

    @DebugLog
    private void handleWorkoutUpdateMsg(IronMessage statusMsg) throws IOException {
        final IronMessage.WorkoutUpdate workoutUpdate = statusMsg.getWorkoutUpdate();
        getAct().onRep(workoutUpdate.getCurrentRep());
    }

    private void handleVideoMsg(IronMessage videoMsg) throws IOException {
        Log.d(TAG, "handleVideoMsg");
        FileUtils.byteArrayToFile(
                FileUtils.getDayFile(getAct(), getUid(), AppConsts.VIDEO_FILENAME),
                videoMsg.getVideo().getVideoData().toByteArray());

        getAct().videoReady(getUid());
        sendMsg(IronMessage.newBuilder());
    }

    @DebugLog
    private void handleJointErrorMsg(IronMessage statusMsg) {
        final ArrayList<Map<String, String>> jointListData = new ArrayList<Map<String, String>>();

        final IronMessage.FormErrorData formError = statusMsg.getErrorData();

        ArrayList<String> currErrors = new ArrayList<String>();
        
        for (IronMessage.Error error : formError.getErrorList()) {
            final String jointMsg = error.getErrorMessage();

            final HashMap<String, String> item = new HashMap<String, String>();
            item.put("name", jointMsg);
            item.put("msg", "");
            jointListData.add(item);

            currErrors.add(jointMsg);
        }
        if (currErrors.size() > 0) {
            Vibrator vib = (Vibrator) getAct().getSystemService(Context.VIBRATOR_SERVICE);
            vib.vibrate(200);
        }
        for (String currError : currErrors) {
            if (!prevErrors.contains(currError)) {
                getAct().tts.speak(currError.replace("_", " "), TextToSpeech.QUEUE_ADD, null);
            }
        }
        prevErrors.clear();
        prevErrors.addAll(currErrors);

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
        if (mSocket == null) {
            return;
        }
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
