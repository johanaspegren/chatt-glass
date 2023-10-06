package com.aie.chatt_glass;

import com.google.gson.Gson;

import java.util.List;

public class ChatMessage {
    private String role;
    private String content;

    public ChatMessage(String role, String content) {
        this.role = role;
        this.content = content;
    }

    public String getRole() {
        return role;
    }

    public String getContent() {
        return content;
    }

    // Add getters and setters if needed

    // A method to serialize a list of ChatMessage objects to a JSON array
    public static String serialize(List<ChatMessage> messages) {
        Gson gson = new Gson();
        return gson.toJson(messages);
    }
}
