package model;

import java.io.Serializable;
import java.time.LocalDateTime;

public class Message implements Serializable {
    private String roomId;
    private String senderPhone;
    private String content;
    private LocalDateTime timestamp;
    private MessageType type;
    private String image; // For image messages
    private byte[] voiceData; // For voice messages

    public Message(String roomId, String senderPhone, String content) {
        this.roomId = roomId;
        this.senderPhone = senderPhone;
        this.content = content;
        this.timestamp = LocalDateTime.now();
        this.type = MessageType.TEXT; // Default type
    }

    public Message(String roomId, String senderPhone, String base64ImageString,MessageType type) {
        this.roomId = roomId;
        this.senderPhone = senderPhone;
        this.image = base64ImageString;
        this.timestamp = LocalDateTime.now();
        this.type = type;
    }

    public Message(String roomId, String senderPhone, byte[] audioData,MessageType type) {
        this.roomId = roomId;
        this.senderPhone = senderPhone;
        this.voiceData = audioData;
        this.timestamp = LocalDateTime.now();
        this.type = type;
    }

    // New constructor with timestamp
    public Message(String roomId, String senderPhone, String content, LocalDateTime timestamp) {
        this.roomId = roomId;
        this.senderPhone = senderPhone;
        this.content = content;
        this.timestamp = timestamp;
    }

    public String getRoomId() { return roomId; }
    public String getSenderPhone() { return senderPhone; }
    public String getContent() { return content; }
    public LocalDateTime getTimestamp() { return timestamp; }
}
