package com.ironeye;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Scanner;

import static com.ironeye.IronEyeProtos.IronMessage;

public class MyServer {
    public static final String PHONE_HOST = MiddleServerProperties.get("phone_host");
    public static final int PHONE_PORT = MiddleServerProperties.getInt("phone_port");

    public static final String SERVER_HOST = MiddleServerProperties.get("server_host");
    public static final int SERVER_PORT = MiddleServerProperties.getInt("server_port");

    private static Socket socketToPhone, socketToServer;

    public static void main(String[] args) throws IOException {
        execAdb();
        if (MiddleServerProperties.getBoolean("use_mock")) {
            runMockServerAsync();
        }
        String id = scanQr();

        socketToPhone = new Socket(PHONE_HOST, PHONE_PORT);
        OutputStream outToPhone = socketToPhone.getOutputStream();
        InputStream inFromPhone = socketToPhone.getInputStream();

        // Connect with scanning user
        IronMessage msg = IronMessage.parseDelimitedFrom(socketToPhone.getInputStream());
        verifyId(id, msg);

        // Create socket with server
        socketToServer = new Socket(SERVER_HOST, SERVER_PORT);
        OutputStream outToServer = socketToServer.getOutputStream();
        InputStream inFromServer = socketToServer.getInputStream();

        // Give server user object
        msg.writeDelimitedTo(outToServer);
        System.out.println("Wrote user info.");

        outToServer.flush();

        // Set up forwarding between phone and server
        outStreamToInStreamAsync(inFromPhone, outToServer);
        outStreamToInStreamAsync(inFromServer, outToPhone);
    }

    private static void runMockServerAsync() {
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    runMockServer();
                } catch (IOException e) {
                    e.printStackTrace();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }).start();
    }

    private static void runMockServer() throws IOException, InterruptedException {
        ServerSocket socket = new ServerSocket(SERVER_PORT);
        final Socket socketToPhone = socket.accept();
        OutputStream outToPhone = socketToPhone.getOutputStream();

        IronMessage.UserInfo userInfo =
                IronMessage.parseDelimitedFrom(socketToPhone.getInputStream()).getUserInfo();

        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    while (true) {
                        IronMessage msg = IronMessage.parseDelimitedFrom(socketToPhone.getInputStream());
                        if (msg.getType().equals(IronMessage.MessageType.SET_START)) {
                            System.out.println("startSet!");
                        }
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }).start();

        for (int i = 0; i < 3; i++) {
            Thread.sleep(5000 - i * 300);

            IronMessage.JointError je1 = IronMessage.JointError.newBuilder()
                    .setJointType(IronMessage.JointType.LEFT_FOOT)
                    .setErrorMessage("Wrong")
                    .build();

            IronMessage.JointError je2 = IronMessage.JointError.newBuilder()
                    .setJointType(IronMessage.JointType.LEFT_FOOT)
                    .setErrorMessage("Wrong")
                    .build();

            ArrayList<IronMessage.JointError> jes = new ArrayList<IronMessage.JointError>();
            jes.add(je1);
            jes.add(je2);

            IronMessage.FormErrorData.Builder fed = IronMessage.FormErrorData.newBuilder()
                    .addAllJoint(jes);

            IronMessage statusMsg = IronMessage.newBuilder()
                    .setType(IronMessage.MessageType.FORM_ERROR)
                    .setErrorData(fed)
                    .build();

            statusMsg.writeDelimitedTo(outToPhone);
        }

        IronMessage.Set set1 = IronMessage.Set.newBuilder()
                .setReps((int) (20 + (Math.random() * 20)))
                .setWeight((int) (20 + (Math.random() * 20)))
                .build();

        IronMessage.Set set2 = IronMessage.Set.newBuilder()
                .setReps((int) (20 + (Math.random() * 20)))
                .setWeight((int) (20 + (Math.random() * 20)))
                .build();

        ArrayList<IronMessage.Set> sets = new ArrayList<IronMessage.Set>();
        sets.add(set1);
        sets.add(set2);

        IronMessage.WorkoutInfo.Builder wi = IronMessage.WorkoutInfo.newBuilder()
                .addAllSet(sets);

        IronMessage workoutMsg = IronMessage.newBuilder()
                .setType(IronMessage.MessageType.WORKOUT_INFO)
                .setWorkoutInfo(wi)
                .build();

        workoutMsg.writeDelimitedTo(outToPhone);

        fileToOutStream(outToPhone, new File("small.mp4"));

        outToPhone.close();
        socketToPhone.close();
        socket.close();
    }

    private static void fileToOutStream(OutputStream outToPhone, File f) throws IOException {
        FileInputStream fis = new FileInputStream(f);
        byte[] buffer = new byte[4096];
        int len;
        while ((len = fis.read(buffer)) != -1) {
            outToPhone.write(buffer, 0, len);
        }
        outToPhone.flush();
        fis.close();
    }

    private static void outStreamToInStreamAsync(final InputStream in, final OutputStream out) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    outStreamToInStream(in, out);
                } catch (IOException e) {
                    e.printStackTrace();
                }
                try {
                    socketToPhone.close();
                    socketToServer.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }).start();
    }

    private static void outStreamToInStream(InputStream in, OutputStream out) throws IOException {
        while (true) {
            int dataIn = in.read();
            if (dataIn == -1) {
                break;
            }
            out.write(dataIn);
            out.flush();
        }
    }

    private static void verifyId(String id, IronMessage msg) {
        boolean isSame =
                msg.getType() == IronMessage.MessageType.USER_INFO &&
                        id.equals(msg.getUserInfo().getId());
        if (!isSame) {
            throw new RuntimeException("ID not consistent with scanned ID.");
        }
    }

    private static String scanQr() throws IOException {
        Process p = Runtime.getRuntime().exec("zbarcam --raw --prescale=700x700");
        BufferedReader qrReader = new BufferedReader(new InputStreamReader(p.getInputStream()));
        String id = qrReader.readLine();
        System.out.println("id = " + id);
        qrReader.close();
        p.destroy();
        return id;
    }

    private static void execAdb() {
        try {
            Process p = Runtime.getRuntime().exec("adb forward tcp:" + PHONE_PORT + " tcp:" + PHONE_PORT);
            Scanner sc = new Scanner(p.getErrorStream());
            if (sc.hasNext()) {
                while (sc.hasNext()) {
                    System.out.println(sc.next());
                }
                throw new RuntimeException("Cannot start the Android debug bridge");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
