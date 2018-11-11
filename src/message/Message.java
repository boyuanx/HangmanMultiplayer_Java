package message;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

public class Message implements Serializable {

    private MessageType type;
    private String username;
    private String message;
    private Map<String, Object> dataMap;

    public Message(String username) {
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

    public void putData(String key, Object value) {
        if (dataMap == null) {
            dataMap = new HashMap<>();
        }
        dataMap.put(key, value);
    }

    public Object getData(String key) {
        return dataMap.get(key);
    }

    public void deleteData(String key) {
        dataMap.remove(key);
    }
}
