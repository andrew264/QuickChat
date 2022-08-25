package com.andrew264;

import org.json.JSONObject;

import java.io.*;
import java.net.Socket;
import java.util.LinkedList;

public class ClientHandler implements Runnable {

    public final static LinkedList<ClientHandler> clientHandlers = new LinkedList<>();
    public final static LinkedList<String> usernames = new LinkedList<>();
    private final static LinkedList<Message> messages = new LinkedList<>();
    private final Socket socket;
    private BufferedReader bufferedReader;
    private BufferedWriter bufferedWriter;
    private String clientUsername;

    public ClientHandler(Socket socket) throws IOException {
        this.socket = socket;
        try {
            this.bufferedWriter = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()));
            this.bufferedReader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            this.clientUsername = bufferedReader.readLine();
            syncMessagesWithClient();
        } catch (IOException e) {
            closeEverything(socket, bufferedReader, bufferedWriter);
        }
        for (ClientHandler client : clientHandlers) {
            if (client.clientUsername.equalsIgnoreCase(this.clientUsername)) {
                JSONObject jsonObject = new JSONObject();
                jsonObject.put("username", "SERVER");
                jsonObject.put("message", "User " + this.clientUsername + " is already connected");
                jsonObject.put("to", this.clientUsername);
                this.bufferedWriter.write(jsonObject.toString());
                this.bufferedWriter.newLine();
                this.bufferedWriter.flush();
                this.socket.close();
                this.bufferedReader.close();
                this.bufferedWriter.close();
                throw new IOException("User [" + this.clientUsername + "] is already connected");
            }
        }
        clientHandlers.add(this);
        usernames.add(this.clientUsername);
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("username", "SERVER");
        jsonObject.put("message", clientUsername + " has joined the chat.");
        jsonObject.put("to", "everyone");
        broadcastMessage(new Message(jsonObject));
    }

    @Override
    public void run() {
        String messageJSONFromClient;

        while (!socket.isClosed()) {
            try {
                messageJSONFromClient = bufferedReader.readLine();
                if (messageJSONFromClient == null || messageJSONFromClient.equalsIgnoreCase("exit")) {
                    closeEverything(socket, bufferedReader, bufferedWriter);
                    break;
                } else {
                    Message msgToSend = new Message(messageJSONFromClient);
                    broadcastMessage(msgToSend);
                }
            } catch (IOException e) {
                closeEverything(socket, bufferedReader, bufferedWriter);
                break;
            }
        }
    }

    public void syncMessagesWithClient() {
        for (Message msg : messages) {
            if (msg.isDM() && !msg.getTo().equals(clientUsername)) continue;
            try {
                bufferedWriter.write(msg.getJSONString());
                bufferedWriter.newLine();
                bufferedWriter.flush();
            } catch (IOException e) {
                closeEverything(socket, bufferedReader, bufferedWriter);
                break;
            }
        }
    }

    public void broadcastMessage(Message message) {
        messages.add(message);
        if (message.isDM()) {
            if (usernames.contains(message.getTo())) {
                for (ClientHandler client : clientHandlers) {
                    if (client.clientUsername.equals(message.getTo())) {
                        try {
                            client.bufferedWriter.write(message.getJSONString());
                            client.bufferedWriter.newLine();
                            client.bufferedWriter.flush();
                        } catch (IOException e) {
                            closeEverything(socket, bufferedReader, bufferedWriter);
                        }
                        break;
                    }
                }
            } else {
                try {
                    JSONObject jsonObject = new JSONObject();
                    jsonObject.put("username", "SERVER");
                    jsonObject.put("message", "User [" + message.getTo() + "] not found");
                    jsonObject.put("to", message.getUsername());
                    bufferedWriter.write(new Message(jsonObject).getJSONString());
                    bufferedWriter.newLine();
                    bufferedWriter.flush();
                } catch (Exception e) {
                    closeEverything(socket, bufferedReader, bufferedWriter);
                }
            }
        } else {
            for (ClientHandler client : clientHandlers) {
                if (!client.clientUsername.equals(clientUsername)) {
                    try {
                        client.bufferedWriter.write(message.getJSONString());
                        client.bufferedWriter.newLine();
                        client.bufferedWriter.flush();
                    } catch (IOException e) {
                        closeEverything(socket, bufferedReader, bufferedWriter);
                    }
                }
            }
        }
    }

    public void removeClientHandler() {
        clientHandlers.remove(this);
        usernames.remove(this.clientUsername);
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("username", "SERVER");
        jsonObject.put("message", clientUsername + " has left the chat.");
        jsonObject.put("to", "everyone");
        broadcastMessage(new Message(jsonObject));
    }

    public void closeEverything(Socket socket, BufferedReader bufferedReader, BufferedWriter bufferedWriter) {
        removeClientHandler();
        Client.closeEverything(socket, bufferedReader, bufferedWriter);
    }
}