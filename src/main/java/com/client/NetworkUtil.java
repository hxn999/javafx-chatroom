package com.client;

import java.io.*;
import java.net.Socket;

public class NetworkUtil {
    private static ObjectInputStream in;
    private static ObjectOutputStream out;
    private static String currentPhone;

    public static void connect(String host, int port) throws IOException {
        Socket socket = new Socket(host, port);
        out = new ObjectOutputStream(socket.getOutputStream());
        in = new ObjectInputStream(socket.getInputStream());
    }

    public static ObjectInputStream getIn() { return in; }
    public static ObjectOutputStream getOut() { return out; }

    public static void setCurrentPhone(String phone) {
        currentPhone = phone;
    }

    public static String getCurrentPhone() {
        return currentPhone;
    }
}
