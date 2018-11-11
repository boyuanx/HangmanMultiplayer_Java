package message;

import java.io.Serializable;

public class Message implements Serializable {

    private MessageType type;
    private String username;
    private String message;

    public Message(String username, String message) {
        this.message = message;
        this.username = username;
    }

    public MessageType getMessageType() {
        return type;
    }

    public void setMessageType(MessageType messageType) {
        type = messageType;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }
}
