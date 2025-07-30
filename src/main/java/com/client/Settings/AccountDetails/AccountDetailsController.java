package com.client.Settings.AccountDetails;


import com.client.Page;
import com.client.Pages;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.KeyCode;
import javafx.scene.shape.Circle;
import javafx.scene.text.Font;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import model.User;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Base64;
import java.util.ResourceBundle;
import com.client.chat.ChatController;
import java.util.concurrent.CompletableFuture;

//import static com.Application.App.currentUser;



public class AccountDetailsController implements Initializable {

    @FXML
    private TextField phoneField;

    @FXML
    private TextField usernameField;

    @FXML
    public Button saveButton;
    @FXML
    public Button backButton;
    @FXML
    private ImageView profileImageView;
    @FXML
    private Button changePhotoButton;
    @FXML
    private TextField newPasswordField;

    private String imageUrl;


    @FXML
    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {

        URL imgUrl = getClass().getResource("/images/account-avatar.png");
        if (imgUrl == null) {
            System.err.println(">>> cannot find /images/account-avatar.png on classpath");
        } else {
            Image img = new Image(imgUrl.toExternalForm());
            profileImageView.setImage(img);
        }

        changePhotoButton.setOnAction(evt -> {
            FileChooser chooser = new FileChooser();
            chooser.setTitle("Select Profile Photo");
            chooser.getExtensionFilters().addAll(
                    new FileChooser.ExtensionFilter("Image Files", "*.png", "*.jpg", "*.jpeg", "*.gif")
            );
            File chosen = chooser.showOpenDialog(profileImageView.getScene().getWindow());
            if (chosen != null) {
                Image img = new Image(chosen.toURI().toString(), 100, 100, true, true);// preserve ratio + smooth
                profileImageView.setImage(img);
                imageUrl = chosen.getAbsolutePath();
            }
        });

        backButton.setOnAction(this::handleBack);

        saveButton.setOnAction(this::handleSave);

    }

    public boolean isValidBDNumber(String phone) {
        if (phone.startsWith("+880")) {
            return phone.length() == 14 && phone.substring(4).chars().allMatch(Character::isDigit);
        }
        if (phone.startsWith("01")) {
            return phone.length() == 11 && phone.chars().allMatch(Character::isDigit);
        }
        return false;
    }


    // Handle the back button action
    public void handleBack(ActionEvent actionEvent) {
        try {
            // Navigate back to the settings page
            new Page().Goto(Pages.SETTINGS);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    public void handleSave(ActionEvent actionEvent) {
        String Phone = phoneField.getText();
        String Username = usernameField.getText();
        String Password = newPasswordField.getText();
        String newimgUrl = imageUrl;

        if (imageUrl != null) {
            try {
                byte[] bytes = Files.readAllBytes(Paths.get(imageUrl));
                newimgUrl = Base64.getEncoder().encodeToString(bytes);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        String msg = "EDIT:" + ChatController.currentUser.getPhoneNumber().trim() + ":" +
                (Phone != null ? Phone.trim() : "") + ":" +
                (Username != null ? Username.trim() : "") + ":" +
                (Password != null ? Password.trim() : "") + ":" +
                (newimgUrl != null ? newimgUrl : "");

        try {
            // Simulate sending the message and receiving a response
            System.out.println("Message sent: " + msg);
            Object response = "SUCCESS"; // Mock response
            if ("SUCCESS".equals(response)) {
                System.out.println("EDIT SUCCESSFUL");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        try {
            new Page().Goto(Pages.SETTINGS);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}

