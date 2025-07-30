package com.client;

import com.client.chat.ChatController;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import model.User;

public class SceneUtil {
    public static void switchScene(Stage stage, String fxmlPath) {
        try {
            FXMLLoader loader = new FXMLLoader(SceneUtil.class.getResource(fxmlPath));
            Parent root = loader.load();
            stage.setScene(new Scene(root));
            stage.show();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

//    public static void loadChatWithRoom(Stage stage, String roomId, User user) {
//        try {
//            FXMLLoader loader = new FXMLLoader(SceneUtil.class.getResource("/chat.fxml"));
//            Parent root = loader.load();
//
//            ChatController controller = loader.getController();
//            controller.setRoomIdAndUser(roomId, user);
//
//            stage.setScene(new Scene(root));
//            stage.show();
//        } catch (Exception e) {
//            e.printStackTrace();
//        }
//    }
}
