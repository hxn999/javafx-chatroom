package model;

import java.io.Serializable;
import java.time.LocalDateTime;

public class Message implements Serializable {
    private String roomId;
    private String senderPhone;
    private String senderName;
    private String content;
    private LocalDateTime timestamp;

    public Message(String roomId, String senderPhone, String senderName, String content, LocalDateTime now) {
        this.roomId = roomId;
        this.senderPhone = senderPhone;
        this.senderName = senderName;
        this.content = content;
        this.timestamp = LocalDateTime.now();
    }

    // New constructor with timestamp
    public Message(String roomId, String senderPhone, String content, LocalDateTime timestamp) {
        this.roomId = roomId;
        this.senderPhone = senderPhone;
        this.content = content;
        this.timestamp = timestamp;
    }

    public String getSenderName() {
        return senderName;
    }

    public String getRoomId() { return roomId; }
    public String getSenderPhone() { return senderPhone; }
    public String getContent() { return content; }
    public LocalDateTime getTimestamp() { return timestamp; }
}
