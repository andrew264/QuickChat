package com.andrew264;

import java.io.IOException;
import java.net.*;
import java.util.logging.Level;
import java.util.logging.Logger;

public class DiscoveryThread implements Runnable {

    DatagramSocket socket;

    public static DiscoveryThread getInstance() {
        return DiscoveryThreadHolder.INSTANCE;
    }

    @Override
    @SuppressWarnings("InfiniteLoopStatement")
    public void run() {
        try {
            //Keep a socket open to listen to all the UDP traffic that is destined for this port
            try {
                socket = new DatagramSocket(8888, InetAddress.getByName("0.0.0.0"));
                System.out.println("Listening on port 8888...");
            } catch (SocketException | UnknownHostException e) {
                System.out.println("Socket exception: " + e.getMessage());
                System.out.println("Socket in use, try closing any other programs that are using port 8888");
                System.exit(1);
            }
            try {
                socket.setBroadcast(true);
            } catch (SocketException e) {
                System.out.println("Socket exception: " + e.getMessage());
            }

            while (true) {
                //Receive a packet
                byte[] receiveBuf = new byte[15000];
                DatagramPacket packet = new DatagramPacket(receiveBuf, receiveBuf.length);
                try {
                    socket.receive(packet);
                } catch (IOException e) {
                    System.out.println("Socket exception: " + e.getMessage());
                }

                //See if the packet holds the right command (message)
                String message = new String(packet.getData()).trim();
                if (message.equals("DISCOVER_CHAT_SERVER_REQUEST")) {
                    byte[] sendData = "DISCOVER_CHAT_SERVER_RESPONSE".getBytes();

                    //Send a response
                    DatagramPacket sendPacket = new DatagramPacket(sendData, sendData.length, packet.getAddress(), packet.getPort());
                    socket.send(sendPacket);

                    System.out.println("Sent packet to: " + sendPacket.getAddress().getHostAddress());
                }
            }
        } catch (IOException ex) {
            Logger.getLogger(DiscoveryThread.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    private static class DiscoveryThreadHolder {
        private static final DiscoveryThread INSTANCE = new DiscoveryThread();
    }
}