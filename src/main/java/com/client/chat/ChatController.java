package com.client.chat;

import com.client.NetworkUtil;
import com.client.Page;
import com.client.Pages;
import com.videoCall.AudioReceiver;
import com.videoCall.AudioSender;
import com.videoCall.VideoReceiver;
import com.videoCall.VideoSender;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.scene.text.Text;
import javafx.scene.text.TextFlow;
import javafx.stage.Stage;
import model.Message;
import model.User;

import java.io.ByteArrayInputStream;
import java.io.ObjectInputStream;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;

public class ChatController {

    public static ChatController chatController;

    @FXML
    private ImageView sendIcon;
    @FXML
    private Button settingsButton;
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
    @FXML
    private Button videoCallBtn;
    @FXML
    public ImageView video;


    private List<Message> messages;
    public static User currentUser;
    public volatile boolean isRecording = false;
    private String base64ImageString;
    public byte[] voiceData;


    // video call threads
    VideoSender videoSenderThread = new VideoSender("127.0.0.1", 5555);
    VideoReceiver videoReceiverThread = new VideoReceiver(video, 5556);
    AudioSender audioSenderThread = new AudioSender("127.0.0.1", 5557);
    AudioReceiver audioReceiverThread = new AudioReceiver(5558);


    public static ImageView getImageViewFromBase64(String base64String) {
        if (base64String == null || base64String.isEmpty()) {
            return null;
        }

        String base64Data = base64String;
        if (base64String.startsWith("data:image")) {
            int commaIndex = base64String.indexOf(',');
            if (commaIndex != -1) {
                base64Data = base64String.substring(commaIndex + 1);
            }
        }

        try {
            byte[] decodedBytes = Base64.getDecoder().decode(base64Data);
            ByteArrayInputStream inputStream = new ByteArrayInputStream(decodedBytes);
            Image image = new Image(inputStream);
            return new ImageView(image);
        } catch (IllegalArgumentException e) {
            System.err.println("Error decoding Base64 string: " + e.getMessage());
            return null;
        }
    }


    @FXML
    private void initialize() {


        chatController = this;
        settingsButton.setOnAction(this::onSettingsClicked);
        sendButton.setOnAction(e -> sendMessage());
        userName.setText(currentUser.getName());
        userImage.setImage(getImageViewFromBase64(currentUser.getBase64ProfilePic()).getImage());
    }

    private void sendMessage() {


        String text = messageField.getText().trim();

        boolean hasImage = base64ImageString != null && !base64ImageString.isEmpty();
        boolean hasText = !text.isEmpty();
//        boolean hasVoice = voiceData.length!=0; // Placeholder for voice message logic
        if (!hasText) return;
        try {
            Message msg = new Message(roomLabel.getText(), currentUser.getPhoneNumber(), currentUser.getName(), text, LocalDateTime.now());

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
                    } else if (obj instanceof String response) {
//                        videoCallBtn.setText(response);
                        if (response.equals("RECEIVE_CALL")) {
                            Platform.runLater(() -> {
                                videoCallBtn.setText("RECEIVE CALL");
                            });
                        } else if (response.equals("FAILED")) {
                            Platform.runLater(() -> {
                                videoCallBtn.setText("VIDEO CALL");
                                showAlert("Failed to call.");
                            });
                        } else if (response.equals("WAITING")) {
                            Platform.runLater(() -> {
                                videoCallBtn.setText("CALLING...");

                            });
                        } else if (response.equals("ACCEPT_CALL")) {
                            Platform.runLater(() -> {
                                videoCallBtn.setText("END CALL");

                                audioSenderThread.start();
                                audioReceiverThread.start();
                                videoSenderThread.start();
                                videoReceiverThread.start();


                            });
                        } else if (response.equals("END_CALL")) {
                            Platform.runLater(() -> {
                                videoCallBtn.setText("VIDEO CALL");

                                // stop all threads
                                videoSenderThread.stopThread();
                                videoReceiverThread.stopThread();
                                audioSenderThread.stopThread();
                                audioReceiverThread.stopThread();


                            });
                        }
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

    @FXML
    private void onSettingsClicked(ActionEvent event) {
        try {
            new Page().Goto(Pages.SETTINGS);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    // Build text content (show sender for others only)
    private void addMessageBubble(String text, boolean mine, LocalTime time, String sender) {
        // HBox to align the whole message bubble left/right
        HBox messageContainer = new HBox();
        messageContainer.setAlignment(mine ? Pos.CENTER_RIGHT : Pos.CENTER_LEFT);
        messageContainer.setPadding(new Insets(5, 10, 5, 10));

        // Message Text (just the text content now)
        Text messageText = new Text(text);
        messageText.setStyle("-fx-fill: " + (mine ? "white" : "black") + "; -fx-font-size: 14;");

        // Message bubble styling
        TextFlow messageBubble = new TextFlow(messageText);
        messageBubble.setPadding(new Insets(10));
        messageBubble.setMaxWidth(300); // Prevent stretching full width
        messageBubble.setStyle(
                mine
                        ? "-fx-background-color: #0084ff; -fx-background-radius: 15 0 15 15;"
                        : "-fx-background-color: #8e24aa; -fx-background-radius: 0 15 15 15;"
        );

        // Timestamp label
        Label timeLabel = new Label(time.format(DateTimeFormatter.ofPattern("hh:mm a")));
        timeLabel.setStyle("-fx-text-fill: " + (mine ? "#aad4ff" : "#dddddd") + "; -fx-font-size: 10;");

        // VBox to stack sender (if not mine), bubble, and time
        VBox messageContent = new VBox(3);
        messageContent.setAlignment(mine ? Pos.CENTER_RIGHT : Pos.CENTER_LEFT);
        messageContent.setMaxWidth(Region.USE_PREF_SIZE);

        // Add sender label above bubble for other users
        if (!mine && sender != null) {
            Label senderLabel = new Label(sender);
            senderLabel.setStyle("-fx-text-fill: #666666; -fx-font-size: 12; -fx-font-weight: bold;");
            messageContent.getChildren().add(senderLabel);
        }

        messageContent.getChildren().addAll(messageBubble, timeLabel);
        HBox.setHgrow(messageContent, Priority.NEVER); // prevent stretching

        // Final add to message container
        messageContainer.getChildren().add(messageContent);
        this.messageContainer.getChildren().add(messageContainer); // <-- your outer VBox holding all messages
    }

    private void addMessageToUI(Message msg) {
        boolean mine = msg.getSenderPhone().equals(currentUser.getPhoneNumber());
        String sender = mine ? "You" : msg.getSenderName();
        LocalTime time = msg.getTimestamp().toLocalTime();
        addMessageBubble(msg.getContent(), mine, time, sender);
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
                messages = null;
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

    public void videoCallHandler(ActionEvent event) {
        try {
            String room = roomLabel.getText();
            if (room == null || room.isEmpty() || room.equals("JOIN A ROOM")) {
                showAlert("Please join a room first.");
                return;
            }

            if (videoCallBtn.getText().equals("RECEIVE CALL")) {
                // Accept incoming call
                NetworkUtil.getOut().writeObject("ACCEPT_CALL:" + room);
                NetworkUtil.getOut().flush();
//                videoCallBtn.setText("IN A CALL");

                //starting all videcall threads


                return;
            } else if (videoCallBtn.getText().equals("END CALL")) {


                NetworkUtil.getOut().writeObject("END_CALL:" + room);
                NetworkUtil.getOut().flush();

            } else {


                // Send video call request
                NetworkUtil.getOut().writeObject("VIDEO_CALL:" + room);
                NetworkUtil.getOut().flush();

                videoCallBtn.setText("CALLING...");
            }


        } catch (Exception e) {
            showAlert("Failed to initiate video call.");
        }
    }
}
