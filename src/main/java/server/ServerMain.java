package server;

import model.Message;
import model.User;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.*;

public class ServerMain {
    public static Map<String, User> users = new HashMap<>();
    public static Map<String, List<Message>> roomHistory = new HashMap<>();
    public static Map<String, List<ObjectOutputStream>> roomClients = new HashMap<>();

    public static void main(String[] args) {
        loadUsersFromFile();
        try (ServerSocket serverSocket = new ServerSocket(6000)) {
            System.out.println("Server started on port 6000...");
            while (true) {
                Socket socket = serverSocket.accept();
                new ClientHandler(socket);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void saveUsersToFile() {
        try (ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream("users.dat"))) {
            oos.writeObject(ServerMain.users);
            System.out.println("User data saved.");
        } catch (IOException e) {
            System.err.println("Failed to save user data: " + e.getMessage());
        }
    }

    public static void loadUsersFromFile() {
        File file = new File("users.dat");
        if (file.exists()) {
            try (ObjectInputStream ois = new ObjectInputStream(new FileInputStream(file))) {
                users = (Map<String, User>) ois.readObject();
                System.out.println("Loaded " + users.size() + " users.");
            } catch (Exception e) {
                System.err.println("Failed to load users: " + e.getMessage());
            }
        } else {
            users = new HashMap<>();
        }
    }


}
