package com.ironeye;

import com.google.protobuf.ByteString;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.file.Files;
import java.nio.file.Paths;
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
        while (true) {
            handleSingleExercise();
        }
    }

    private static void handleSingleExercise() throws IOException {
        ArrayList<Thread> threads = new ArrayList<Thread>();

        execAdb();
        if (MiddleServerProperties.getBoolean("use_mock")) {
            threads.add(runMockServerAsync());
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
        threads.add(outStreamToInStreamAsync(inFromPhone, outToServer));
        threads.add(outStreamToInStreamAsync(inFromServer, outToPhone));

        for (Thread thread : threads) {
            try {
                thread.join();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

        System.out.println("DONE");
        System.out.println();
    }

    private static Thread runMockServerAsync() {
        Thread t = new Thread(new Runnable() {
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
        });
        t.start();
        return t;
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
                        IronMessage msg = IronMessage
                                .parseDelimitedFrom(socketToPhone.getInputStream());
                        System.out.println(msg);
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }).start();

        for (int i = 0; i < 3; i++) {
            Thread.sleep(3000 - i * 300);

            if (i == 0) {
                IronMessage.newBuilder()
                        .setType(IronMessage.MessageType.SET_START)
                        .build()
                        .writeDelimitedTo(outToPhone);
            } else if (i == 1) {
                IronMessage.newBuilder()
                        .setType(IronMessage.MessageType.SET_END)
                        .build()
                        .writeDelimitedTo(outToPhone);
            }

            IronMessage.Error.Builder err1 = IronMessage.Error.newBuilder()
                    .setErrorMessage("Bar is too tilted");

            IronMessage.Error.Builder err2 = IronMessage.Error.newBuilder()
                    .setErrorMessage("Bring knees forward");

            IronMessage.FormErrorData.Builder fed = IronMessage.FormErrorData.newBuilder()
                    .addError(err1)
                    .addError(err2);

            IronMessage statusMsg = IronMessage.newBuilder()
                    .setType(IronMessage.MessageType.FORM_ERROR)
                    .setErrorData(fed)
                    .build();

            statusMsg.writeDelimitedTo(outToPhone);
        }

        ArrayList<IronMessage.Set> sets = new ArrayList<IronMessage.Set>();
        sets.add(randomSet());
        sets.add(randomSet());
        sets.add(randomSet());

        IronMessage.WorkoutInfo.Builder wi = IronMessage.WorkoutInfo.newBuilder()
                .addAllSet(sets);

        IronMessage workoutMsg = IronMessage.newBuilder()
                .setType(IronMessage.MessageType.WORKOUT_INFO)
                .setWorkoutInfo(wi)
                .build();

        workoutMsg.writeDelimitedTo(outToPhone);

        IronMessage.Video.Builder vidInfo = IronMessage.Video.newBuilder()
                .setVideoData(ByteString.copyFrom(Files.readAllBytes(Paths.get("small.mp4"))));

        IronMessage vidMsg = IronMessage.newBuilder()
                .setType(IronMessage.MessageType.VIDEO)
                .setVideo(vidInfo)
                .build();

        vidMsg.writeDelimitedTo(outToPhone);

        outToPhone.close();
        socketToPhone.close();
        socket.close();
    }

    private static IronMessage.Set randomSet() {
        return IronMessage.Set.newBuilder()
                .setReps((int) (20 + (Math.random() * 20)))
                .setWeight((int) (20 + (Math.random() * 20)))
                .build();
    }

    private static Thread outStreamToInStreamAsync(final InputStream in, final OutputStream out) {
        Thread t = new Thread(new Runnable() {
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
        });
        t.start();
        return t;
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
        String s;
        if (MiddleServerProperties.getBoolean("use_zbarcam")) {
            s = MiddleServerProperties.get("zbarcam_cmd");
        } else {
            s = MiddleServerProperties.get("zbarimg_cmd");
        }
        Process p = Runtime.getRuntime().exec(s);
        BufferedReader qrReader = new BufferedReader(new InputStreamReader(p.getInputStream()));
        String id = qrReader.readLine();
        System.out.println("id = " + id);
        qrReader.close();
        p.destroy();
        return id;
    }

    private static void execAdb() {
        try {
            Process p = Runtime.getRuntime()
                    .exec("adb forward tcp:" + PHONE_PORT + " tcp:" + PHONE_PORT);
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
