package com.andrew264;

import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.net.*;
import java.util.Enumeration;
import java.util.logging.Level;
import java.util.logging.Logger;

public class FindServer {

    public static @Nullable InetAddress getIP() {
        InetAddress ip = null;
        // Find the server using UDP broadcast
        try {
            //Open a random port to send the package
            DatagramSocket c = new DatagramSocket();
            c.setBroadcast(true);
            c.setSoTimeout(10 * 1000);

            byte[] sendData = "DISCOVER_CHAT_SERVER_REQUEST".getBytes();

            //Try the 255.255.255.255 first
            try {
                DatagramPacket sendPacket = new DatagramPacket(sendData, sendData.length, InetAddress.getByName("255.255.255.255"), 8888);
                c.send(sendPacket);
            } catch (Exception ignored) {
            }

            // Broadcast the message over all the network interfaces
            Enumeration<NetworkInterface> interfaces = NetworkInterface.getNetworkInterfaces();
            while (interfaces.hasMoreElements()) {
                NetworkInterface networkInterface = interfaces.nextElement();

                if (networkInterface.isLoopback() || !networkInterface.isUp() || networkInterface.isVirtual()) {
                    continue; // Don't want to broadcast to the loopback interface or the interfaces that are Virtual
                }

                for (InterfaceAddress interfaceAddress : networkInterface.getInterfaceAddresses()) {
                    InetAddress broadcast = interfaceAddress.getBroadcast();
                    if (broadcast == null) {
                        continue;
                    }

                    // Send the broadcast package!
                    try {
                        DatagramPacket sendPacket = new DatagramPacket(sendData, sendData.length, broadcast, 8888);
                        c.send(sendPacket);
                    } catch (Exception ignored) {
                    }
                }
            }

            System.out.println("Waiting for a reply from Server!");

            //Wait for a response
            byte[] receiveBuf = new byte[15000];
            DatagramPacket receivePacket = new DatagramPacket(receiveBuf, receiveBuf.length);
            try {
                c.receive(receivePacket);
            } catch (SocketTimeoutException e) {
                System.out.println("Timeout: No response from Server!");
                c.close();
                return null;
            }

            //We have a response
            System.out.println("Broadcast response from server: " + receivePacket.getAddress().getHostAddress());

            //Check if the message is correct
            String message = new String(receivePacket.getData()).trim();
            if (message.equals("DISCOVER_CHAT_SERVER_RESPONSE")) {
                ip = receivePacket.getAddress();
            }

            //Close the port!
            c.close();
        } catch (IOException ex) {
            Logger.getLogger(FindServer.class.getName()).log(Level.SEVERE, null, ex);
        }
        return ip;
    }
}
