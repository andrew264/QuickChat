package com.andrew264;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

public class Server implements Runnable {
    private static final int serverPort = 42069;
    private final ServerSocket serverSocket;

    public Server() throws IOException {
        this.serverSocket = new ServerSocket(serverPort);
    }

    @Override
    public void run() {
        Thread discoveryThread = new Thread(DiscoveryThread.getInstance());
        discoveryThread.start();
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
            try {
                serverSocket.close();
            } catch (IOException ex) {
                System.out.println(ex.getMessage());
            }
            System.out.println("Server exception: " + e.getMessage());
        }
    }
}
