package com.andrew264;

import org.json.JSONObject;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatterBuilder;

public class Message {
    private final String message;
    private final JSONObject jsonObject;
    private final long timestamp;
    private final String username;
    private final String to;

    public Message(JSONObject jsonObject) {
        this.jsonObject = jsonObject;
        this.username = (String) jsonObject.get("username");
        this.message = (String) jsonObject.get("message");
        this.to = (String) jsonObject.get("to");
        this.timestamp = Instant.now().getEpochSecond();
        jsonObject.put("timestamp", String.valueOf(this.timestamp));
    }

    public Message(String jsonString) {
        this.jsonObject = new JSONObject(jsonString);
        this.username = (String) jsonObject.get("username");
        this.message = (String) jsonObject.get("message");
        this.to = (String) jsonObject.get("to");
        this.timestamp = Long.parseLong((String) jsonObject.get("timestamp"));
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
        Instant instant = Instant.ofEpochSecond(timestamp);
        LocalDateTime dateTime = LocalDateTime.ofInstant(instant, java.time.ZoneId.systemDefault());
        return dateTime.format(new DateTimeFormatterBuilder().appendPattern("h:mm a").toFormatter());
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

}
