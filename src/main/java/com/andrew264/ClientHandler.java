package com.andrew264;
import org.json.JSONObject;

import java.io.*;
import java.net.Socket;
import java.util.ArrayList;

public class ClientHandler implements Runnable {

    public final static ArrayList<ClientHandler> clientHandlers = new ArrayList<>();
    public final static ArrayList<String> usernames = new ArrayList<>();
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
        System.out.println("[" + clientUsername + "]: connected from " +
                socket.getInetAddress().getHostAddress() + ":" + socket.getPort() + ".");
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

    public void broadcastMessage(Message message) {
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
                        } finally {
                            System.out.println(message);
                        }
                        break;
                    }
                }
            } else {
                System.out.println("User [" + message.getTo() + "] Not Found");
                try {
                    JSONObject jsonObject = new JSONObject();
                    jsonObject.put("username", "SERVER");
                    jsonObject.put("message", "User [" + message.getTo() + "] not found");
                    jsonObject.put("to", message.getUsername());
                    bufferedWriter.write(jsonObject.toString());
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
            System.out.println(message);
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
        System.out.println("[" + clientUsername + "]: disconnected from " +
                socket.getInetAddress().getHostAddress() + ":" + socket.getPort() + ".");
    }

    public void closeEverything(Socket socket, BufferedReader bufferedReader, BufferedWriter bufferedWriter) {
        removeClientHandler();
        try {
            if (socket != null) {
                socket.close();
            }
            if (bufferedReader != null) {
                bufferedReader.close();
            }
            if (bufferedWriter != null) {
                bufferedWriter.close();
            }
        } catch (IOException e) {
            System.out.println("Client exception: " + e.getMessage());
        }
    }
}
