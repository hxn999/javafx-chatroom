package server;

import model.User;

import java.io.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class UserDatabase {
    private static final String FILE = "users.dat";
    private HashMap<String, User> users = new HashMap<>();

    public UserDatabase() {
        load();
    }

    @SuppressWarnings("unchecked")
    private void load() {
        try (ObjectInputStream ois = new ObjectInputStream(new FileInputStream(FILE))) {
            users = (HashMap<String, User>) ois.readObject();
        } catch (Exception e) {
            System.out.println("No existing user database found. Starting fresh.");
            users = new HashMap<>();
        }
    }
    public List<User> getAllUsers() {
        return new ArrayList<>(users.values());
    }

    public void save() {
        try (ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(FILE))) {
            oos.writeObject(users);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public boolean addUser(User user) {
        if (users.containsKey(user.getPhoneNumber())) return false;
        users.put(user.getPhoneNumber(), user);
        save();
        return true;
    }

    public User getUser(String username) {
        return users.get(username);
    }

    public HashMap<String, User> gethAllUsers() {
        return users;
    }
}
