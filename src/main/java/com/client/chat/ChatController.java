package com.client.chat;

import com.client.NetworkUtil;
import com.client.Page;
import com.client.Pages;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.VBox;
import model.Message;
import model.User;

import java.io.ObjectInputStream;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

public class ChatController {

    @FXML
    private VBox messageContainer;
    @FXML
    private TextField messageField;
    @FXML
    private Button sendButton;
    @FXML
    private Label userName;
    @FXML
    private ImageView userImage;
    @FXML
    private Button joinRoomBtn;
    @FXML
    private Button createRoomBtn;
    @FXML
    private Button leaveRoomBtn;
    @FXML
    private TextField roomId;
    @FXML
    private TextField createRoomId;
    @FXML
    private Label roomLabel;

    private List<Message> messages;


    public static User currentUser;

//    public void setRoomIdAndUser(String roomId, User user) {
//        this.roomId.setText(roomId);
//        this.currentUser = user;
//
//        userName.setText(user.getName());
//        if (user.getBase64ProfilePic() != null) {
//            Image image = new Image("data:image/png;base64," + user.getBase64ProfilePic());
//            userImage.setImage(image);
//        }
//
//        listenForMessages();
//        loadRoomHistory();
//    }





    @FXML
    private void initialize() {
        sendButton.setOnAction(e -> sendMessage());
    }

    private void sendMessage() {
        String text = messageField.getText().trim();
        if (text.isEmpty()) return;

        try {
            Message msg = new Message(roomLabel.getText(), currentUser.getPhoneNumber(), text, LocalDateTime.now());

            NetworkUtil.getOut().writeObject(msg);
            NetworkUtil.getOut().flush();

            messageField.clear();
        } catch (Exception e) {
            e.printStackTrace();
            showAlert("Failed to send message.");
        }
    }

    private void listenForMessages() {
        Thread thread = new Thread(() -> {
            try {
                ObjectInputStream in = NetworkUtil.getIn();
                while (true) {
                    Object obj = in.readObject();
                    if (obj instanceof Message msg && msg.getRoomId().equals(roomId.getText())) {
                        messages.add(msg);
                        Platform.runLater(() -> addMessageToUI(msg));
                    }
                }
            } catch (Exception e) {
                Platform.runLater(() -> showAlert("Disconnected from server."));
            }
        });
        thread.setDaemon(true);
        thread.start();
    }

    private void loadRoomHistory() throws Exception {

        Object obj = NetworkUtil.getIn().readObject();
        if (obj instanceof List<?> list) {
            messages = (List<Message>) list;

        } else {
            if (obj instanceof String response) {
                if (response.startsWith("JOINED:")) {
                    messages = new ArrayList<>();
                    System.out.println("Joined new created  room successfully.");
                } else if (response.startsWith("FAIL")) {
                    showAlert("Failed to join room. Room does not exist.");
                } else {
                    showAlert("Unexpected response: " + response);
                }
            }
        }

    }

    private void addMessageToUI(Message msg) {
        String sender = msg.getSenderPhone().equals(currentUser.getPhoneNumber()) ? "You" : msg.getSenderPhone();
        String timestamp = msg.getTimestamp().format(DateTimeFormatter.ofPattern("HH:mm"));
        String display = sender + " [" + timestamp + "]: " + msg.getContent();

        Label label = new Label(display);
        label.setWrapText(true);
        messageContainer.getChildren().add(label);
    }

    private void populateMessages() {
        messageContainer.getChildren().clear();
        if (messages != null) {
            if (messages.size() == 0) {
                System.out.println("No messages in this room yet.");
                return;
            }
            for (Message msg : messages) {
                addMessageToUI(msg);
            }
        }
    }

    private void showAlert(String msg) {
        new Alert(Alert.AlertType.ERROR, msg).showAndWait();
    }

    @FXML
    private void loginBackHandler(ActionEvent event) {
        // Handle back to login
        try {
            NetworkUtil.getOut().writeObject("LOGOUT:" + currentUser.getPhoneNumber());
            NetworkUtil.getOut().flush();
        } catch (Exception e) {
            showAlert("Failed to logout.");
        }

        // Switch to login scene
        try {
            new Page().Goto(Pages.LOGIN);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

    }

    public void createRoomHandler(ActionEvent event) {
        String newRoomId = createRoomId.getText().trim();
        if (newRoomId.isEmpty()) {
            showAlert("Room ID cannot be empty.");
            return;
        }

        try {
            NetworkUtil.getOut().writeObject("CREATE_ROOM:" + newRoomId);
            NetworkUtil.getOut().flush();
//            roomLabel.setText("Current Room: " + roomId);
            loadRoomHistory();
            roomLabel.setText(newRoomId);
            populateMessages();
        } catch (Exception e) {
            showAlert("Failed to create room.");
        }
    }

    public void joinRoomHandler(ActionEvent event) {
        String joiningRoomId = roomId.getText().trim();
        if (joiningRoomId.isEmpty()) {
            showAlert("Room ID cannot be empty.");
            return;
        }

        try {
            NetworkUtil.getOut().writeObject("JOIN:" + joiningRoomId);
            NetworkUtil.getOut().flush();
//            roomLabel.setText("Current Room: " + roomId);
            loadRoomHistory();
            roomLabel.setText(joiningRoomId);
            populateMessages();
            listenForMessages();
        } catch (Exception e) {
            showAlert("Failed to join room.");
        }
    }

    public void leaveRoomHandler(ActionEvent event) {
        try {
            NetworkUtil.getOut().writeObject("LEAVE:");
            NetworkUtil.getOut().flush();

            // Clear the message container
            messageContainer.getChildren().clear();
            // Reset room ID and label
            String response = (String) NetworkUtil.getIn().readObject();

            if (response.equals("LEFT")) {
                showAlert("You have left the room.");
                messages=null;
                roomLabel.setText("JOIN A ROOM");
                populateMessages();
            } else {
                System.out.println("Response: " + response);
                showAlert("Failed to leave room.");
                return;
            }


        } catch (Exception e) {
            showAlert("Failed to leave room.");
        }
    }
}
