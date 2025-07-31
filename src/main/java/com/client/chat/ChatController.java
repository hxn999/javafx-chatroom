package com.client.chat;

import com.client.NetworkUtil;
import com.client.Page;
import com.client.Pages;
import com.videoCall.AudioReceiver;
import com.videoCall.AudioSender;
import com.videoCall.VideoReceiver;
import com.videoCall.VideoSender;
import com.voiceMessage.VoicePlayback;
import com.voiceMessage.VoiceRecorder;
import com.voiceMessage.VoiceUtil;
import javafx.animation.*;
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
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Rectangle;
import javafx.scene.text.Text;
import javafx.scene.text.TextFlow;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.util.Duration;
import model.Message;
import model.MessageType;
import model.User;

import javax.sound.midi.Sequencer;
import javax.sound.sampled.LineUnavailableException;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.nio.file.Files;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.concurrent.CompletableFuture;

public class ChatController {

    public static ChatController chatController;

    @FXML
    private ImageView VoiceSend;
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
    public volatile boolean  isRecording = false;
    private String base64ImageString ;
    public  byte[] voiceData;
    public volatile boolean isMsgListening = false;
    public String currentRoomId;


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

        leaveRoomBtn.setVisible(false);
        videoCallBtn.setVisible(false);
        chatController = this;
        VoiceSend.setOnMouseClicked(event -> {recordVoice();});
        sendIcon.setOnMouseClicked(event -> selectImage());
        settingsButton.setOnAction(this::onSettingsClicked);
        sendButton.setOnAction(e -> sendMessage());
        userName.setText(currentUser.getName());
        userImage.setImage(getImageViewFromBase64(currentUser.getBase64ProfilePic()).getImage());
    }

    private void sendMessage() {


        String text = messageField.getText().trim();

        boolean hasImage = base64ImageString != null && !base64ImageString.isEmpty();
        boolean hasText = !text.isEmpty();
        boolean hasVoice = voiceData != null && voiceData.length != 0;// Placeholder for voice message logic
        if (!hasText && !hasImage && !hasVoice) return;

        if(hasText) {
            try {
                Message msg = new Message(roomLabel.getText(), currentUser.getPhoneNumber(), currentUser.getName(), text, LocalDateTime.now());

                NetworkUtil.getOut().writeObject(msg);
                NetworkUtil.getOut().flush();
                System.out.println("Received Message: " + msg);

                messageField.clear();
            } catch (Exception e) {
                e.printStackTrace();
                showAlert("Failed to send message.");
            }
        }
        if(hasImage) {
            try {
                Message msg = new Message(roomLabel.getText(), currentUser.getPhoneNumber(), base64ImageString, MessageType.IMAGE);
                NetworkUtil.getOut().writeObject(msg);
                NetworkUtil.getOut().flush();
            } catch (IOException e) {
                e.printStackTrace();
                showAlert("Failed to send Image.");
            }
        }
        if(hasVoice){
            try{
                Message msg = new Message(roomLabel.getText(), currentUser.getPhoneNumber(), voiceData, MessageType.VOICE);
                NetworkUtil.getOut().writeObject(msg);
                NetworkUtil.getOut().flush();
            } catch (IOException e) {
                e.printStackTrace();
                showAlert("Failed to send Image.");
            }

        }
    }

    private Thread messageListenerThread;

    private void startMessageListener() {
        stopListenForMessages(); // Stop any previous listener
        isMsgListening = true;
        messageListenerThread = new Thread(() -> {
            try {
                while (isMsgListening) {
                    ObjectInputStream in = NetworkUtil.getIn(); // Always get a fresh stream reference
                    Object obj = in.readObject();

                    if (obj instanceof Message msg && msg.getRoomId().equals(currentRoomId)) {
                        System.out.println("Received Message: " + msg.getContent());
                        System.out.println("before adding");
                        messages.add(msg);
                        System.out.println("Adding message to UI: " + msg.getContent());
                        Platform.runLater(() -> {
                            System.out.println("Adding message from: " + msg.getSenderName() + " -> " + msg.getContent());
                            addMessageToUI(msg);
                        });
                    } else if (obj instanceof String response) {
//                        videoCallBtn.setText(response);
                        System.out.println(response);
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
                        }else if (response.equals("LEFT")) {
                            isMsgListening = false; // ⬅️ Stop loop early outside UI thread

                            Platform.runLater(() -> {
                                showAlert("You have left the room.");
                                messages = null;
                                messageContainer.getChildren().clear(); // clear chat view
                                roomLabel.setText("JOIN A ROOM");
                                currentRoomId = null;
                                leaveRoomBtn.setVisible(false);
                                videoCallBtn.setVisible(false);
                                joinRoomBtn.setDisable(false);
                                createRoomBtn.setDisable(false);
                                populateMessages(); // reset display
                            });

                            break;
                        }
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
                Platform.runLater(() -> showAlert("Disconnected from server."));
            }finally {
                isMsgListening = false; // Ensure we stop listening
                messageListenerThread = null; // Clear reference
            }
        });
        messageListenerThread.setDaemon(true);
        messageListenerThread.start();
    }

    private void listenForMessages() {
        startMessageListener();
    }

    private void stopListenForMessages() {
        isMsgListening = false;
        if (messageListenerThread != null && messageListenerThread.isAlive()) {
            messageListenerThread.interrupt();
        }
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

    public void addMessageToUI(Message msg) {
            boolean mine = msg.getSenderPhone().equals(currentUser.getPhoneNumber());
            String sender = mine ? "You" : msg.getSenderName();
            LocalTime time = msg.getTimestamp().toLocalTime();
            if(msg.getType().equals(MessageType.IMAGE)) {
                addImageBubble(msg.getImage(), mine, time, sender);
            }else if(msg.getType().equals(MessageType.VOICE)) {
                addVoiceBubble(msg.getVoiceData(), mine, time, sender);
            }
            else {
                System.out.println("Reached in the addMessageToUI.....................");
                addMessageBubble(msg.getContent(), mine, time, sender);
            }
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
//            loadRoomHistory();
//            roomLabel.setText(newRoomId);
//            populateMessages();
            String response = (String) NetworkUtil.getIn().readObject();
            if (response.startsWith("CREATED:")) {
                System.out.println("Room created successfully.");
            }
            else {
                showAlert("Failed to create room.");
            }



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
            loadRoomHistory();
            roomLabel.setText(joiningRoomId);
            currentRoomId = joiningRoomId;
            leaveRoomBtn.setVisible(true);
            videoCallBtn.setVisible(true);
            joinRoomBtn.setDisable(true);
            createRoomBtn.setDisable(true);
            populateMessages();
            listenForMessages(); // This now always starts a fresh listener
            createRoomId.clear();
            roomId.clear();
        } catch (Exception e) {
            showAlert("Failed to join room.");
        }
    }

    //To play Voice
    private void addVoiceBubble(byte[] audioData, boolean mine, LocalTime time, String sender) {
        // HBox to align the whole message bubble left/right
        HBox messageContainer = new HBox();
        messageContainer.setAlignment(mine ? Pos.CENTER_RIGHT : Pos.CENTER_LEFT);
        messageContainer.setPadding(new Insets(5, 10, 5, 10));

        // Voice control elements
        HBox voiceControls = new HBox(10);
        voiceControls.setAlignment(Pos.CENTER_LEFT);
        voiceControls.setPadding(new Insets(10));

        // Play button
        Button playButton = new Button("▶");
        playButton.setStyle("-fx-font-size: 16px; -fx-background-color: transparent; -fx-text-fill: " +
                (mine ? "white" : "black") + "; -fx-cursor: hand;");

        // Voice duration/status label
        Label durationLabel = new Label("Voice Message");
        durationLabel.setStyle("-fx-text-fill: " + (mine ? "white" : "black") + "; -fx-font-size: 12px;");

        // Waveform representation (simple visual)
        Label waveformLabel = new Label("♪ ♫ ♪ ♫ ♪");
        waveformLabel.setStyle("-fx-text-fill: " + (mine ? "#aad4ff" : "#bbbbbb") + "; -fx-font-size: 10px;");

        voiceControls.getChildren().addAll(playButton, durationLabel, waveformLabel);

        // Message bubble styling for voice
        VBox voiceBubble = new VBox(voiceControls);
        voiceBubble.setPadding(new Insets(5));
        voiceBubble.setMaxWidth(250);
        voiceBubble.setStyle(
                mine
                        ? "-fx-background-color: #0084ff; -fx-background-radius: 15 0 15 15;"
                        : "-fx-background-color: #8e24aa; -fx-background-radius: 0 15 15 15;"
        );

        // Play button functionality
        playButton.setOnAction(e -> {
            if (playButton.getText().equals("▶")) {
                playButton.setText("⏸");
                durationLabel.setText("Playing...");
                VoicePlayback.playAudio(audioData);

                // Simulate playback duration
//                new Thread(() -> {
//                    try {
//                        Thread.sleep(3000); // Simulate 3 seconds of playback
//                        Platform.runLater(() -> {
//                            playButton.setText("▶");
//                            durationLabel.setText("Voice Message");
//                        });
//                    } catch (InterruptedException ex) {
//                        Thread.currentThread().interrupt();
//                    }
//                }).start();
            } else {
                playButton.setText("▶");
                durationLabel.setText("Voice Message");
                // TODO: Stop voice playback
            }
        });

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

        messageContent.getChildren().addAll(voiceBubble, timeLabel);
        HBox.setHgrow(messageContent, Priority.NEVER); // prevent stretching

        // Final add to message container
        messageContainer.getChildren().add(messageContent);
        this.messageContainer.getChildren().add(messageContainer);
    }


    //To show image
    private void addImageBubble(String base64Image, boolean mine, LocalTime time, String sender) {
        // HBox to align the whole message bubble left/right
        HBox messageContainer = new HBox();
        messageContainer.setAlignment(mine ? Pos.CENTER_RIGHT : Pos.CENTER_LEFT);
        messageContainer.setPadding(new Insets(5, 10, 5, 10));

        // Image view from base64
        ImageView imageView = getImageViewFromBase64(base64Image);
        if (imageView == null) {
            // Fallback to text if image can't be loaded
            addMessageBubble("Image could not be loaded", mine, time, sender);
            return;
        }

        // Set image size constraints
        imageView.setFitWidth(200);
        imageView.setFitHeight(150);
        imageView.setPreserveRatio(true);
        imageView.setSmooth(true);

        // Message bubble styling for image
        VBox imageBubble = new VBox(imageView);
        imageBubble.setPadding(new Insets(5));
        imageBubble.setMaxWidth(220);
        imageBubble.setStyle(
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

        messageContent.getChildren().addAll(imageBubble, timeLabel);
        HBox.setHgrow(messageContent, Priority.NEVER); // prevent stretching

        // Final add to message container
        messageContainer.getChildren().add(messageContent);
        this.messageContainer.getChildren().add(messageContainer); // <-- your outer VBox holding all messages
    }

    private void selectImage() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Select Image");
        fileChooser.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("Image Files", "*.png", "*.jpg", "*.jpeg", "*.gif", "*.bmp")
        );

        Stage stage = (Stage) sendIcon.getScene().getWindow();
        File file = fileChooser.showOpenDialog(stage);

        if (file != null) {
            try {
                byte[] imageBytes = Files.readAllBytes(file.toPath());
                base64ImageString = Base64.getEncoder().encodeToString(imageBytes);
                showAlert("Image selected successfully!");
            } catch (Exception e) {
                showAlert("Failed to load image: " + e.getMessage());
            }
        }
    }

    private void recordVoice() {
        if (!VoiceUtil.isRecording()) {
            VoiceUtil.setRecording(true);
            VoiceSend.setStyle("-fx-background-color: red;"); // optional: visual feedback
            VoiceRecorder.captureAudio();
        } else {
            VoiceUtil.setRecording(false);
            VoiceSend.setStyle("-fx-background-color: red;"); // reset
            VoiceUtil.setRecording(false);  // stop the capture
            new Thread(() -> {
                try {
                    Thread.sleep(500);
                    voiceData = VoiceRecorder.getAudioByteArray();
                    VoicePlayback.playAudio(voiceData);// wait for recording to finish
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }).start();

//            System.out.println(voiceData.length);

//            if (audio != null && audio.length > 0) {
////                sendVoiceMessage(audio);
//            }
        }
    }

    public void leaveRoomHandler(ActionEvent event) {
        try {
            if(currentRoomId==null) {
                showAlert("You are not in any room.");
                return;
            }
//            System.out.println("running stuck");
//            stopListenForMessages();
//            System.out.println("not stuck");

            NetworkUtil.getOut().writeObject("LEAVE:");
            NetworkUtil.getOut().flush();

            // Clear the message container
//            messageContainer.getChildren().clear();
            // Reset room ID and label
//            String response = (String) NetworkUtil.getIn().readObject();

//            if (response.equals("LEFT")) {
//                showAlert("You have left the room.");
//                messages = null;
//                roomLabel.setText("JOIN A ROOM");
//                currentRoomId = null;
//                leaveRoomBtn.setVisible(false);
//                joinRoomBtn.setDisable(false);
//                createRoomBtn.setDisable(false);
//                populateMessages();
//            } else {
//                System.out.println("Response: " + response);
//                showAlert("Failed to leave room.");
//                return;
//            }


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
