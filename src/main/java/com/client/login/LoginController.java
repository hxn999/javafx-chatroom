package com.client.login;

import com.client.NetworkUtil;
import com.client.Page;
import com.client.Pages;
import com.client.SceneUtil;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.stage.Stage;
import model.User;
import com.client.chat.ChatController;
public class LoginController {
    @FXML private TextField phone;
    @FXML private TextField password;
    @FXML private Button login;
    @FXML private Button createAccountButton;

    @FXML
    private void initialize() {
        createAccountButton.setOnAction(e -> {
            Stage stage = (Stage) createAccountButton.getScene().getWindow();
            try {
                new Page().Goto(Pages.CREATE_ACCOUNT);
            } catch (Exception ex) {
                throw new RuntimeException(ex);
            }
        });
    }

    @FXML
    private void loginHandler() {
        try {
            NetworkUtil.connect("127.0.0.1", 6000);
            String msg = "LOGIN:" + phone.getText().trim() + ":" + password.getText().trim();
            NetworkUtil.getOut().writeObject(msg);
            NetworkUtil.getOut().flush();

            Object response = NetworkUtil.getIn().readObject();
            System.out.println(response);
            if ("SUCCESS".equals(response)) {
                User user = (User) NetworkUtil.getIn().readObject();
                NetworkUtil.setCurrentPhone(user.getPhoneNumber());

                ChatController.currentUser = user;

                new Page().Goto(Pages.CHAT);
            } else {
                new Alert(Alert.AlertType.ERROR, "Invalid credentials").showAndWait();
            }

        } catch (Exception e) {
            new Alert(Alert.AlertType.ERROR, "Connection error").showAndWait();
        }
    }
}
