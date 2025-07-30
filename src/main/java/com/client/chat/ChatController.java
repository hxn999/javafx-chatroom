package com.client.chat;

import com.client.NetworkUtil;
import com.client.Page;
import com.client.Pages;
import com.voiceMessage.VoicePlayback;
import com.voiceMessage.VoiceRecorder;
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

    @FXML
    private ImageView VoiceSend;
    @FXML
    private  ImageView sendIcon;
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


    private List<Message> messages;
    public static User currentUser;
    public volatile boolean  isRecording = false;
    private String base64ImageString ;
    public byte[] voiceData;


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
//        VoiceSend.setOnMouseClicked(event -> {recordVoice();});
        sendIcon.setOnMouseClicked(event -> selectImage());
        settingsButton.setOnAction(this::onSettingsClicked);
        sendButton.setOnAction(e -> sendMessage());
        userName.setText(currentUser.getName());
        userImage.setImage(getImageViewFromBase64(currentUser.getBase64ProfilePic()).getImage());
    }

    private void sendMessage() {


        String text = messageField.getText().trim();

        boolean hasImage = base64ImageString!=null && !base64ImageString.isEmpty();
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

    //To play Voice
    private void addVoiceBubble(byte[] voiceData, boolean mine, LocalTime time, String sender) {
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
                VoicePlayback.playAudio(voiceData);

                // Simulate playback duration
                new Thread(() -> {
                    try {
                        Thread.sleep(3000); // Simulate 3 seconds of playback
                        Platform.runLater(() -> {
                            playButton.setText("▶");
                            durationLabel.setText("Voice Message");
                        });
                    } catch (InterruptedException ex) {
                        Thread.currentThread().interrupt();
                    }
                }).start();
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

//    private void recordVoice() {
//        // Create custom dialog
//        Dialog<Void> recordingDialog = new Dialog<>();
//        recordingDialog.setTitle("Voice Message");
//        recordingDialog.initStyle(StageStyle.UNDECORATED);
//
//        // Main container with gradient background
//        VBox mainContainer = new VBox();
//        mainContainer.setPrefSize(400, 300);
//        mainContainer.setStyle(
//                "-fx-background-color: linear-gradient(135deg, #667eea 0%, #764ba2 100%);" +
//                        "-fx-background-radius: 20;" +
//                        "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.3), 20, 0, 0, 5);"
//        );
//
//        // Header with close button
//        HBox header = new HBox();
//        header.setAlignment(Pos.CENTER_RIGHT);
//        header.setPadding(new Insets(15, 20, 0, 20));
//
//        Button closeButton = new Button("✕");
//        closeButton.setStyle(
//                "-fx-background-color: #ff4444;" +
//                        "-fx-background-radius: 15;" +
//                        "-fx-text-fill: white;" +
//                        "-fx-font-size: 16px;" +
//                        "-fx-font-weight: bold;" +
//                        "-fx-cursor: hand;" +
//                        "-fx-padding: 5 10 5 10;" +
//                        "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.3), 5, 0, 0, 2);"
//        );
//        closeButton.setOnMouseEntered(e -> closeButton.setStyle(
//                "-fx-background-color: #ff6666;" +
//                        "-fx-background-radius: 15;" +
//                        "-fx-text-fill: white;" +
//                        "-fx-font-size: 16px;" +
//                        "-fx-font-weight: bold;" +
//                        "-fx-cursor: hand;" +
//                        "-fx-padding: 5 10 5 10;" +
//                        "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.4), 8, 0, 0, 3);"
//        ));
//        closeButton.setOnMouseExited(e -> closeButton.setStyle(
//                "-fx-background-color: #ff4444;" +
//                        "-fx-background-radius: 15;" +
//                        "-fx-text-fill: white;" +
//                        "-fx-font-size: 16px;" +
//                        "-fx-font-weight: bold;" +
//                        "-fx-cursor: hand;" +
//                        "-fx-padding: 5 10 5 10;" +
//                        "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.3), 5, 0, 0, 2);"
//        ));
//        closeButton.setOnAction(e -> {
//            // Stop all animations immediately;
//            if (VoiceRecorder.isRecording()) {
//                VoiceRecorder.setRecording(false);
//            }
//            recordingDialog.close();
//            // Force stop recording if active
//            if (VoiceRecorder.isRecording()) {
//                VoiceRecorder.setRecording(false);
//            }
//        });
//        header.getChildren().add(closeButton);
//
//        // Content area
//        VBox content = new VBox(20);
//        content.setAlignment(Pos.CENTER);
//        content.setPadding(new Insets(20, 40, 40, 40));
//
//        // Title
//        Label titleLabel = new Label("🎤 Voice Message");
//        titleLabel.setStyle(
//                "-fx-font-size: 24px;" +
//                        "-fx-font-weight: bold;" +
//                        "-fx-text-fill: white;" +
//                        "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.5), 2, 0, 0, 1);"
//        );
//
//        // Status label with smooth transitions
//        Label statusLabel = new Label("Tap and hold to record");
//        statusLabel.setStyle(
//                "-fx-font-size: 16px;" +
//                        "-fx-text-fill: rgba(255,255,255,0.9);" +
//                        "-fx-text-alignment: center;"
//        );
//
//        // Recording button with animated effects
//        Button recordButton = new Button();
//        recordButton.setPrefSize(120, 120);
//        recordButton.setStyle(
//                "-fx-background-color: rgba(255,255,255,0.2);" +
//                        "-fx-background-radius: 60;" +
//                        "-fx-border-color: rgba(255,255,255,0.3);" +
//                        "-fx-border-width: 2;" +
//                        "-fx-border-radius: 60;" +
//                        "-fx-cursor: hand;" +
//                        "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.3), 15, 0, 0, 3);"
//        );
//
//        // Microphone icon
//        Label micIcon = new Label("🎤");
//        micIcon.setStyle(
//                "-fx-font-size: 40px;" +
//                        "-fx-text-fill: white;"
//        );
//        recordButton.setGraphic(micIcon);
//
//        // Simplified progress ring (less resource intensive)
//        Circle progressRing = new Circle(65);
//        progressRing.setFill(Color.TRANSPARENT);
//        progressRing.setStroke(Color.web("#ffffff", 0.4));
//        progressRing.setStrokeWidth(3);
//        progressRing.setVisible(false);
//
//        // Simplified audio level indicator (fewer bars for better performance)
//        HBox audioLevelContainer = new HBox(3);
//        audioLevelContainer.setAlignment(Pos.CENTER);
//        audioLevelContainer.setVisible(false);
//
//        Rectangle[] levelBars = new Rectangle[3]; // Reduced from 5 to 3 for better performance
//        for (int i = 0; i < levelBars.length; i++) {
//            levelBars[i] = new Rectangle(6, 15 + i * 10);
//            levelBars[i].setFill(Color.web("#4CAF50"));
//            levelBars[i].setArcWidth(3);
//            levelBars[i].setArcHeight(3);
//            audioLevelContainer.getChildren().add(levelBars[i]);
//        }
//
//        // Timer label
//        Label timerLabel = new Label("00:00");
//        timerLabel.setStyle(
//                "-fx-font-size: 18px;" +
//                        "-fx-font-weight: bold;" +
//                        "-fx-text-fill: white;" +
//                        "-fx-background-color: rgba(0,0,0,0.3);" +
//                        "-fx-background-radius: 15;" +
//                        "-fx-padding: 8 16 8 16;"
//        );
//        timerLabel.setVisible(false);
//
//        // Animation references for cleanup
//        Timeline pulseAnimation = null;
//        Timeline ringRotation = null;
//        Timeline audioLevelAnimation = null;
//        Timeline timer = null;
//
//        // Optimized animations with fewer keyframes
//        pulseAnimation = new Timeline(
//                new KeyFrame(Duration.ZERO, new KeyValue(recordButton.scaleXProperty(), 1.0)),
//                new KeyFrame(Duration.millis(800), new KeyValue(recordButton.scaleXProperty(), 1.05)),
//                new KeyFrame(Duration.millis(1600), new KeyValue(recordButton.scaleXProperty(), 1.0))
//        );
//        pulseAnimation.setCycleCount(Timeline.INDEFINITE);
//
//        ringRotation = new Timeline(
//                new KeyFrame(Duration.ZERO, new KeyValue(progressRing.rotateProperty(), 0)),
//                new KeyFrame(Duration.seconds(3), new KeyValue(progressRing.rotateProperty(), 360))
//        );
//        ringRotation.setCycleCount(Timeline.INDEFINITE);
//
//        // Simplified audio level animation with longer intervals
//        audioLevelAnimation = new Timeline();
//        for (int i = 0; i < levelBars.length; i++) {
//            final int index = i;
//            KeyFrame kf = new KeyFrame(
//                    Duration.millis(200 + i * 100), // Increased interval
//                    e -> {
//                        double height = 15 + Math.random() * 25;
//                        levelBars[index].setHeight(height);
//                        levelBars[index].setFill(Color.web(height > 30 ? "#FF5722" : "#4CAF50"));
//                    }
//            );
//            audioLevelAnimation.getKeyFrames().add(kf);
//        }
//        audioLevelAnimation.setCycleCount(Timeline.INDEFINITE);
//
//        // Timer with reference for cleanup
//        timer = new Timeline();
//        final int[] seconds = {0};
//        timer.getKeyFrames().add(new KeyFrame(Duration.seconds(1), e -> {
//            seconds[0]++;
//            int mins = seconds[0] / 60;
//            int secs = seconds[0] % 60;
//            timerLabel.setText(String.format("%02d:%02d", mins, secs));
//        }));
//        timer.setCycleCount(Timeline.INDEFINITE);
//
//        // Store animation references for cleanup
//        final Timeline finalPulseAnimation = pulseAnimation;
//        final Timeline finalRingRotation = ringRotation;
//        final Timeline finalAudioLevelAnimation = audioLevelAnimation;
//        final Timeline finalTimer = timer;
//
//        // Method to stop all animations
//        Runnable stopAllAnimations = () -> {
//            if (finalPulseAnimation != null) finalPulseAnimation.stop();
//            if (finalRingRotation != null) finalRingRotation.stop();
//            if (finalAudioLevelAnimation != null) finalAudioLevelAnimation.stop();
//            if (finalTimer != null) finalTimer.stop();
//        };
//
//        // Button event handlers with async processing
//        recordButton.setOnMousePressed(event -> {
//            // Start recording asynchronously
//            CompletableFuture.runAsync(() -> {
//                VoiceRecorder.setRecording(true);
//                VoiceRecorder.captureAudio();
//            });
//
//            // Update UI immediately
//            Platform.runLater(() -> {
//                statusLabel.setText("Recording... Release to stop");
//                recordButton.setStyle(
//                        "-fx-background-color: rgba(244,67,54,0.8);" +
//                                "-fx-background-radius: 60;" +
//                                "-fx-border-color: rgba(255,255,255,0.5);" +
//                                "-fx-border-width: 3;" +
//                                "-fx-border-radius: 60;" +
//                                "-fx-cursor: hand;" +
//                                "-fx-effect: dropshadow(gaussian, rgba(244,67,54,0.5), 20, 0, 0, 3);"
//                );
//                micIcon.setText("⏹");
//
//                // Show recording indicators
//                progressRing.setVisible(true);
//                audioLevelContainer.setVisible(true);
//                timerLabel.setVisible(true);
//
//                // Start animations
//                finalPulseAnimation.play();
//                finalRingRotation.play();
//                finalAudioLevelAnimation.play();
//                finalTimer.play();
//                seconds[0] = 0;
//            });
//        });
//
//        recordButton.setOnMouseReleased(event -> {
//            // Stop animations immediately to prevent hanging
//            stopAllAnimations.run();
//
//            // Update UI first
//            statusLabel.setText("Processing voice message...");
//            recordButton.setStyle(
//                    "-fx-background-color: rgba(76,175,80,0.8);" +
//                            "-fx-background-radius: 60;" +
//                            "-fx-border-color: rgba(255,255,255,0.5);" +
//                            "-fx-border-width: 2;" +
//                            "-fx-border-radius: 60;" +
//                            "-fx-cursor: hand;" +
//                            "-fx-effect: dropshadow(gaussian, rgba(76,175,80,0.5), 15, 0, 0, 3);"
//            );
//            micIcon.setText("✓");
//
//            // Hide recording indicators
//            progressRing.setVisible(false);
//            audioLevelContainer.setVisible(false);
//
//            // Process recording asynchronously to prevent hanging
//            CompletableFuture.supplyAsync(() -> {
//                VoiceRecorder.setRecording(false);
//                return VoiceRecorder.getAudioByteArray();
//            }).thenAcceptAsync(audioData -> {
//                // Update UI on JavaFX thread
//                Platform.runLater(() -> {
//                    voiceData = audioData;
//                    if (voiceData != null && voiceData.length > 0) {
//                        statusLabel.setText("Voice message ready! 🎉");
//
//                        // Simple success feedback without heavy animations
//                        statusLabel.setOpacity(0.7);
//                        FadeTransition fadeIn = new FadeTransition(Duration.millis(200), statusLabel);
//                        fadeIn.setFromValue(0.7);
//                        fadeIn.setToValue(1.0);
//                        fadeIn.play();
//
//                        // Quick auto-close
//                        Timeline countdown = new Timeline();
//                        final int[] count = {2}; // Reduced countdown time
//                        countdown.getKeyFrames().add(new KeyFrame(Duration.seconds(1), e -> {
//                            count[0]--;
//                            if (count[0] > 0) {
//                                statusLabel.setText("Closing in " + count[0] + "...");
//                            } else {
//                                recordingDialog.close();
//                            }
//                        }));
//                        countdown.setCycleCount(2);
//                        countdown.play();
//                    } else {
//                        statusLabel.setText("No audio detected. Try again!");
//                        recordButton.setStyle(
//                                "-fx-background-color: rgba(255,255,255,0.2);" +
//                                        "-fx-background-radius: 60;" +
//                                        "-fx-border-color: rgba(255,255,255,0.3);" +
//                                        "-fx-border-width: 2;" +
//                                        "-fx-border-radius: 60;" +
//                                        "-fx-cursor: hand;" +
//                                        "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.3), 15, 0, 0, 3);"
//                        );
//                        micIcon.setText("🎤");
//                        timerLabel.setVisible(false);
//                    }
//                });
//            }).exceptionally(throwable -> {
//                // Handle errors gracefully
//                Platform.runLater(() -> {
//                    statusLabel.setText("Error processing audio. Try again!");
//                    recordButton.setStyle(
//                            "-fx-background-color: rgba(255,255,255,0.2);" +
//                                    "-fx-background-radius: 60;" +
//                                    "-fx-border-color: rgba(255,255,255,0.3);" +
//                                    "-fx-border-width: 2;" +
//                                    "-fx-border-radius: 60;" +
//                                    "-fx-cursor: hand;" +
//                                    "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.3), 15, 0, 0, 3);"
//                    );
//                    micIcon.setText("🎤");
//                    timerLabel.setVisible(false);
//                });
//                return null;
//            });
//        });
//
//        // Stack button with progress ring
//        StackPane buttonStack = new StackPane();
//        buttonStack.getChildren().addAll(progressRing, recordButton, audioLevelContainer);
//
//        // Assemble content
//        content.getChildren().addAll(
//                titleLabel,
//                statusLabel,
//                buttonStack,
//                timerLabel
//        );
//
//        mainContainer.getChildren().addAll(header, content);
//
//        // Simplified entrance animation
//        FadeTransition fadeIn = new FadeTransition(Duration.millis(200), mainContainer);
//        fadeIn.setFromValue(0.0);
//        fadeIn.setToValue(1.0);
//
//        ScaleTransition scaleIn = new ScaleTransition(Duration.millis(200), mainContainer);
//        scaleIn.setFromX(0.9);
//        scaleIn.setFromY(0.9);
//        scaleIn.setToX(1.0);
//        scaleIn.setToY(1.0);
//
//        ParallelTransition entrance = new ParallelTransition(fadeIn, scaleIn);
//
//        recordingDialog.getDialogPane().setContent(mainContainer);
//        recordingDialog.getDialogPane().setStyle("-fx-background-color: transparent;");
//
//        // Cleanup on dialog close
//        recordingDialog.setOnCloseRequest(e -> {
//            stopAllAnimations.run();
//            if (VoiceRecorder.isRecording()) {
//                VoiceRecorder.setRecording(false);
//            }
//        });
//
//        recordingDialog.show(); // Use show() instead of showAndWait() to prevent blocking
//        entrance.play();
//    }

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
