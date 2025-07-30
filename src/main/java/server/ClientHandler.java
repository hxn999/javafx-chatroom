package server;

import model.Message;
import model.User;

import java.io.*;
import java.net.Socket;
import java.util.*;

import static server.ServerMain.saveUsersToFile;

public class ClientHandler extends Thread {
    private Socket socket;
    private ObjectInputStream in;
    private ObjectOutputStream out;
    private User currentUser;
    private String currentRoom;

    public ClientHandler(Socket socket) {
        try {
            this.socket = socket;
            this.out = new ObjectOutputStream(socket.getOutputStream());
            this.in = new ObjectInputStream(socket.getInputStream());
            this.start();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void run() {
        try {
            while (true) {
                Object obj = in.readObject();
                if (obj instanceof String command) {
                    if (command.startsWith("LOGIN:")) {
                        handleLogin(command);
                    } else if (command.startsWith("JOIN:")) {
                        handleJoin(command);
                    } else if (command.startsWith("CREATE:")) {
                        handleCreateAccount(command);
                    } else if (command.startsWith("CREATE_ROOM:")) {
                        createRoom(command);
                    } else if (command.startsWith("LEAVE:")) {
                        leaveRoom();
                    }
                } else if (obj instanceof Message msg) {
                    System.out.println("Received message: " + msg.getContent() + " from room: " + msg.getRoomId());
//                    ServerMain.roomHistory.computeIfAbsent(msg.getRoomId(), k -> new ArrayList<>()).add(msg);
                    // After receiving a Message `msg` from client
                    new ChatDatabase().saveRoomMessage(msg); // Add this
                    broadcastToRoom(msg);

                }
            }
        } catch (Exception e) {
            if (currentRoom != null) {
                ServerMain.roomClients.get(currentRoom).remove(out);
            }
            System.out.println("Client disconnected");
        }
    }

    private void handleLogin(String command) throws IOException {
        String[] parts = command.split(":", 3);
        String phone = parts[1];
        String pass = parts[2];

        User user = ServerMain.users.get(phone);
        if (user != null && user.getPassword().equals(pass)) {
            currentUser = user;
            out.writeObject("SUCCESS");
            out.writeObject(user);
        } else {
            out.writeObject("FAIL");
        }
    }

    private void handleCreateAccount(String command) throws IOException {
        String[] parts = command.split(":", 5);
        String phone = parts[1];
        String name = parts[2];
        String pass = parts[3];
        String base64 = parts[4];

        if (ServerMain.users.containsKey(phone)) {
            out.writeObject("EXISTS");
        } else {
            User u = new User(phone, name, pass, base64);
            ServerMain.users.put(phone, u);
            saveUsersToFile(); // ✅ Save all users
            out.writeObject("CREATED");
        }
    }

    private void handleJoin(String command) throws IOException {
        String[] parts = command.split(":", 2);
//        currentRoom = parts[1];
        String roomId = parts[1];
        if (!ServerMain.roomHistory.containsKey(roomId)) {
            out.writeObject("FAIL");
            return;
        }
//        ServerMain.roomClients.computeIfAbsent(roomId, k -> new ArrayList<>()).add(out);

        currentRoom = roomId;
        ServerMain.roomClients.get(roomId).add(out);
        // Send chat history

        List<Message> history = new ChatDatabase().loadRoomChat(currentRoom);
        out.writeObject(history);

    }

    private void broadcastToRoom(Message msg) {
        List<ObjectOutputStream> clients = ServerMain.roomClients.get(msg.getRoomId());
        for (ObjectOutputStream o : clients) {



                try {
                    o.writeObject(msg);
                    System.out.println("Broadcasting message to room: " + msg.getRoomId() + " - " + msg.getContent());
                    o.flush();
                } catch (IOException ignored) {
                }

        }
    }

    private void createRoom(String command) {
        String[] parts = command.split(":", 2);
        String roomId = parts[1];
        try {
            // Initialize room history if not exists
            if (!ServerMain.roomHistory.containsKey(roomId)) {
                File file = new File("room-" + roomId + ".dat");
                if (file.exists()) {
                    try (ObjectInputStream in = new ObjectInputStream(new FileInputStream(file))) {
                        List<Message> messages = (List<Message>) in.readObject();
                        ServerMain.roomHistory.put(roomId, messages);
                    } catch (Exception e) {
                        ServerMain.roomHistory.put(roomId, new ArrayList<>());
                    }
                } else {
                    file.createNewFile();
                    try (ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(file))) {
                        oos.writeObject(new ArrayList<Message>());
                    }
                    ServerMain.roomHistory.put(roomId, new ArrayList<>());
                }
            }

            // Initialize room clients list if not exists
            ServerMain.roomClients.putIfAbsent(roomId, new ArrayList<>());

            // Add this client’s output stream to the room
            ServerMain.roomClients.get(roomId).add(out);


            // Notify the client that they have joined the room
            out.writeObject("JOINED:" + roomId);
            out.flush();


            System.out.println("Client joined room: " + roomId);

        } catch (IOException e) {
            try {
                out.writeObject("FAIL");
            } catch (IOException ex) {
                throw new RuntimeException(ex);
            }
            System.err.println("Failed to create or join room: " + roomId);
            e.printStackTrace();
        }
    }

    public void leaveRoom() {
        if (currentRoom != null && ServerMain.roomClients.containsKey(currentRoom)) {
            ServerMain.roomClients.get(currentRoom).remove(out);
            currentRoom = null;
            try {
                out.writeObject("LEFT");
            } catch (IOException e) {
                e.printStackTrace();

            }
        }
        try {
            out.writeObject("ERROR");
        } catch (IOException e) {
            e.printStackTrace();
        }

    }
}
