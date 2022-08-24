package com.andrew264;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

public class Server {
    private static final int serverPort = 42069;
    private final ServerSocket serverSocket;

    public Server(ServerSocket serverSocket) {
        this.serverSocket = serverSocket;
    }

    public static void main(String[] args) {
        Thread discoveryThread = new Thread(DiscoveryThread.getInstance());
        discoveryThread.start();
        try {
            ServerSocket serverSocket = new ServerSocket(serverPort);
            Server server = new Server(serverSocket);
            server.startServer();
        } catch (IOException e) {
            System.out.println("Server exception: " + e.getMessage());
        }
    }

    public void startServer() throws IOException {
        try {
            while (!serverSocket.isClosed()) {
                Socket socket = serverSocket.accept();
                try {
                    Thread clientHandler = new Thread(new ClientHandler(socket));
                    clientHandler.start();
                } catch (IOException e) {
                    System.out.println(e.getMessage());
                }
            }
        } catch (IOException e) {
            closeServerSocket();
            System.out.println("Server exception: " + e.getMessage());
        }
    }

    public void closeServerSocket() throws IOException {
        if (serverSocket != null) {
            serverSocket.close();
        }
    }
}