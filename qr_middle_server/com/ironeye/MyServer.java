package com.ironeye;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Scanner;

import static com.ironeye.IronEyeProtos.IronMessage;

public class MyServer {
    public static void main(String[] args) throws IOException {
        Socket socketToPhone, socketToServer;

        execAdb();
        runMockServerAsync();
        String id = scanQr();

        socketToPhone = new Socket("localhost", 38300);
        OutputStream outToPhone = socketToPhone.getOutputStream();
        InputStream inFromPhone = socketToPhone.getInputStream();

        // Connect with scanning user
        IronMessage msg = IronMessage.parseDelimitedFrom(socketToPhone.getInputStream());
        verifyId(id, msg);

        // Create socket with server
        socketToServer = new Socket("localhost", 3333);
        OutputStream outToServer = socketToServer.getOutputStream();
        InputStream inFromServer = socketToServer.getInputStream();

        // Give server user object
        msg.writeDelimitedTo(outToServer);
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
        ServerSocket socket = new ServerSocket(3333);
        Socket socketToPhone = socket.accept();

        IronMessage.UserInfo userInfo =
                IronMessage.parseDelimitedFrom(socketToPhone.getInputStream()).getUserInfo();

        for (int i = 0; i < 10; i++) {
            Thread.sleep(5000 - i * 300);

            IronMessage.FormErrorData fed = IronMessage.FormErrorData.newBuilder()
                    .setCommand("Bad knee!")
                    .build();

            IronMessage statusMsg = IronMessage.newBuilder()
                    .setType(IronMessage.MessageType.FORM_ERROR)
                    .setErrorData(fed)
                    .build();

            statusMsg.writeDelimitedTo(socketToPhone.getOutputStream());
        }

        IronMessage.WorkoutInfo fed = IronMessage.WorkoutInfo.newBuilder()
                .setReps((int) (20 + (Math.random() * 20)))
                .setSets((int) (20 + (Math.random() * 20)))
                .setWeight((int) (20 + (Math.random() * 20)))
                .build();

        IronMessage workoutMsg = IronMessage.newBuilder()
                .setType(IronMessage.MessageType.WORKOUT_INFO)
                .setWorkoutInfo(fed)
                .build();

        workoutMsg.writeDelimitedTo(socketToPhone.getOutputStream());
        socketToPhone.close();
        socket.close();
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
            Process p = Runtime.getRuntime().exec("adb forward tcp:38300 tcp:38300");
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
