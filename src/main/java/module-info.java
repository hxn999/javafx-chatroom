module com.client {
    requires javafx.controls;
    requires javafx.fxml;
    requires java.base;
    requires java.desktop;
    requires webcam.capture;

    exports com.client;
    exports com.client.login;
    exports com.client.createAccount;
    exports com.client.chat;
    exports model;

    opens com.client to javafx.fxml;
    opens com.client.login to javafx.fxml;
    opens com.client.createAccount to javafx.fxml;
    opens com.client.chat to javafx.fxml;
    opens model to javafx.base;
}
