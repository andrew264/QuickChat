package com.andrew264;
import org.json.JSONObject;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatterBuilder;

public class Message {
    private final String message;
    private final JSONObject jsonObject;
    private final LocalDateTime time = LocalDateTime.now();
    private String username;
    private String to;

    public Message(JSONObject jsonObject) {
        this.jsonObject = jsonObject;
        this.username = (String) jsonObject.get("username");
        this.message = (String) jsonObject.get("message");
        this.to = (String) jsonObject.get("to");
    }

    public Message(String jsonString) {
        this.jsonObject = new JSONObject(jsonString);
        this.username = (String) jsonObject.get("username");
        this.message = (String) jsonObject.get("message");
        this.to = (String) jsonObject.get("to");
    }

    public String getUsername() {
        return username;
    }

    public String getJSONString() {
        return jsonObject.toString();
    }

    public String getMessage() {
        return message;
    }

    public String getTime() {
        return time.format(new DateTimeFormatterBuilder().appendPattern("h:mm a").toFormatter());
    }

    public String getTo() {
        return to;
    }

    public Boolean isDM() {
        return !to.equals("everyone");
    }

    public String toString() {
        if (isDM()) {
            return "[" + username + " To " + to + "]: @" + getTime() + " : " + message;
        } else {
            return "[" + username + "] @ " + getTime() + " : " + message;
        }
    }

    public void asFPP(String username) {
        if (username.equals(this.username)) {
            this.username = "YOU";
        }
        if (username.equals(this.to)) {
            this.to = "YOU";
        }
    }

}
