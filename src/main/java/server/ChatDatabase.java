package server;

import model.Message;

import java.io.*;
import java.util.ArrayList;
import java.util.List;

public class ChatDatabase {

    private String getRoomFile(String roomId) {
        return "room-" + roomId + ".dat";
    }

    public List<Message> loadRoomChat(String roomId) {
        String file = getRoomFile(roomId);
        try (ObjectInputStream ois = new ObjectInputStream(new FileInputStream(file))) {
            return (List<Message>) ois.readObject();
        } catch (Exception e) {
            return new ArrayList<>();
        }
    }

    public void saveRoomMessage(Message message) {
        String file = getRoomFile(message.getRoomId());
        List<Message> messages = loadRoomChat(message.getRoomId());
        messages.add(message);

        try (ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(file))) {
            oos.writeObject(messages);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // Create a new room file if it doesn't exist
    public void createRoomFile(String roomId) {
        String file = getRoomFile(roomId);
        File f = new File(file);
        if (!f.exists()) {
            try {
                f.createNewFile();
                try (ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(f))) {
                    oos.writeObject(new ArrayList<Message>());
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}
