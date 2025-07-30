package com.client.createAccount;

import com.client.NetworkUtil;
import com.client.SceneUtil;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.image.ImageView;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import model.User;

import java.io.*;
import java.util.Base64;

public class CreateAccountController {

    @FXML private TextField nameField;
    @FXML private TextField phoneField;
    @FXML private PasswordField passwordField;
    @FXML private Button choosePicBtn;
    @FXML private Button createBtn;
    @FXML private Button loginBackBtn;
    @FXML private ImageView avatarPreview;

    private String base64Pic = "";

    @FXML
    private void initialize() {
        choosePicBtn.setOnAction(e -> handleChoosePicture());
        loginBackBtn.setOnAction(e -> {
            Stage stage = (Stage) loginBackBtn.getScene().getWindow();
            SceneUtil.switchScene(stage, "/login.fxml");
        });
    }

    @FXML
    private void loginBackHandler() {
        // TODO: Switch to login scene
        Stage stage = (Stage) loginBackBtn.getScene().getWindow();
        SceneUtil.switchScene(stage, "/login.fxml");
    }

    @FXML
    private void createBtnHandler() {
        try {
            String name = nameField.getText().trim();
            String phone = phoneField.getText().trim();
            String pass = passwordField.getText().trim();

            if (name.isEmpty() || phone.isEmpty() || pass.isEmpty()) {
                showError("All fields required.");
                return;
            }

            NetworkUtil.connect("127.0.0.1", 6000);
            ObjectOutputStream out = NetworkUtil.getOut();
            ObjectInputStream in = NetworkUtil.getIn();

            String command = "CREATE:" + phone + ":" + name + ":" + pass + ":" + base64Pic;
            out.writeObject(command);
            out.flush();

            Object response = in.readObject();
            if ("CREATED".equals(response)) {
                showSuccess("Account created. Go to login.");
                Stage stage = (Stage) createBtn.getScene().getWindow();
                SceneUtil.switchScene(stage, "/login.fxml");
            } else {
                showError("Phone already registered.");
            }

        } catch (Exception e) {
            showError("Registration failed: " + e.getMessage());
        }
    }

    private void handleChoosePicture() {
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Choose Picture");
        File file = chooser.showOpenDialog(new Stage());
        if (file != null) {
            try {
                FileInputStream fis = new FileInputStream(file);
                byte[] bytes = fis.readAllBytes();
                base64Pic = Base64.getEncoder().encodeToString(bytes);
                avatarPreview.setImage(new javafx.scene.image.Image(file.toURI().toString()));
            } catch (IOException e) {
                showError("Failed to load picture.");
            }
        }
    }

    private void showError(String msg) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Error");
        alert.setContentText(msg);
        alert.showAndWait();
    }

    private void showSuccess(String msg) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Success");
        alert.setContentText(msg);
        alert.showAndWait();
    }

    @FXML
    private void joinRoomHandler(){

    }
    @FXML
    private void createRoomHandler() {}
}