package com.andrew264;

import org.json.JSONObject;

import javax.swing.*;
import javax.swing.text.BadLocationException;
import javax.swing.text.Document;
import javax.swing.text.SimpleAttributeSet;
import javax.swing.text.StyleConstants;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.*;
import java.net.InetAddress;
import java.net.Socket;
import java.util.ConcurrentModificationException;
import java.util.LinkedList;

public class Client extends JFrame implements ActionListener {
    private static final int serverPort = 42069;
    private final Socket socket;
    private final String username;
    private final LinkedList<Message> messages = new LinkedList<>();
    private final JTextPane readField;
    private final JTextField writeField;
    private final SimpleAttributeSet timeAttr, usernameAttr, dmAttr, messageAttr;
    private BufferedReader bufferedReader;
    private BufferedWriter bufferedWriter;


    public Client(Socket socket, String username) {
        this.socket = socket;
        this.username = username;
        try {
            this.bufferedWriter = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()));
            this.bufferedReader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            this.bufferedWriter.write(username);
            this.bufferedWriter.newLine();
            this.bufferedWriter.flush();
        } catch (IOException e) {
            closeEverything(socket, bufferedReader, bufferedWriter);
        }

        try {
            for (javax.swing.UIManager.LookAndFeelInfo info : javax.swing.UIManager.getInstalledLookAndFeels()) {
                if ("Windows".equals(info.getName())) {
                    javax.swing.UIManager.setLookAndFeel(info.getClassName());
                    break;
                }
            }
        } catch (Exception ignored) {
        }

        JPanel contentPanel = new JPanel();
        contentPanel.setLayout(new BorderLayout());

        // initialize and setup incoming message text area
        readField = new JTextPane();
        readField.setEditable(false);
        readField.setFont(new Font("Segoe UI", Font.PLAIN, 12)); // set window's default font

        // time Attributes
        timeAttr = new SimpleAttributeSet();
        StyleConstants.setForeground(timeAttr, Color.GRAY);
        StyleConstants.setFontSize(timeAttr, 8);

        // username Attributes
        usernameAttr = new SimpleAttributeSet();
        StyleConstants.setForeground(usernameAttr, Color.RED);
        StyleConstants.setBold(usernameAttr, true);

        // dm Attributes
        dmAttr = new SimpleAttributeSet();
        StyleConstants.setForeground(dmAttr, Color.BLUE);
        StyleConstants.setBold(dmAttr, true);

        // message Attributes
        messageAttr = new SimpleAttributeSet();
        StyleConstants.setForeground(messageAttr, Color.BLACK);


        JScrollPane readFieldPane = new JScrollPane(readField);
        readFieldPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        readFieldPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);

        // initialize and setup outgoing message text field
        writeField = new JTextField();
        writeField.setFont(new Font("Segoe UI", Font.PLAIN, 12)); // set window's default font
        writeField.addActionListener(this);
        writeField.requestFocus();

        // add components to content panel
        contentPanel.add(readFieldPane, BorderLayout.CENTER);
        contentPanel.add(writeField, BorderLayout.SOUTH);

        // add components to frame
        setTitle("Local Chat [" + username + "]");
        getContentPane().add(contentPanel);
        pack();
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        setMinimumSize(new Dimension(550, 400));
        setVisible(true);
    }

    public static void main(String[] args) throws IOException {
        InetAddress serverIP = FindServer.getIP();
        if (serverIP == null) {
            Thread server = new Thread(new Server());
            server.start();
            serverIP = FindServer.getIP();
            if (serverIP == null) System.exit(0);
        }
        String username = (String) JOptionPane.showInputDialog(null, "Enter your username:",
                "Username", JOptionPane.QUESTION_MESSAGE, null, null, "");
        if (username == null || username.isEmpty()) {
            JOptionPane.showMessageDialog(null, "You must enter a username to continue.",
                    "Error", JOptionPane.ERROR_MESSAGE);
            System.exit(0);
        } else if (username.length() > 20) {
            JOptionPane.showMessageDialog(null, "Your username must be less than 20 characters.",
                    "Error", JOptionPane.ERROR_MESSAGE);
            System.exit(0);
        } else if (username.contains(" ")) {
            JOptionPane.showMessageDialog(null, "Your username cannot contain spaces.",
                    "Error", JOptionPane.ERROR_MESSAGE);
            System.exit(0);
        } else if (username.equalsIgnoreCase("server") || username.equalsIgnoreCase("admin") ||
                username.equalsIgnoreCase("everyone") || username.equalsIgnoreCase("you")) {
            JOptionPane.showMessageDialog(null, "Your username cannot be " + username + ".",
                    "Error", JOptionPane.ERROR_MESSAGE);
            System.exit(0);
        }
        try {
            Socket socket = new Socket(serverIP, serverPort);
            Client client = new Client(socket, username);
            client.listenForMessages();
        } catch (IOException e) {
            System.out.println("Client exception: " + e.getMessage());
        }
    }

    public static void closeEverything(Socket socket, BufferedReader bufferedReader, BufferedWriter bufferedWriter) {
        try {
            if (bufferedReader != null) {
                bufferedReader.close();
            }
            if (bufferedWriter != null) {
                bufferedWriter.close();
            }
            if (socket != null) {
                socket.close();
            }
        } catch (IOException e) {
            System.out.println("Client exception: " + e.getMessage());
        }
    }

    public void listenForMessages() {
        new Thread(() -> {
            String receivedJSONString;

            while (socket.isConnected()) {
                try {
                    receivedJSONString = bufferedReader.readLine();
                    if (receivedJSONString == null) {
                        closeEverything(socket, bufferedReader, bufferedWriter);
                        break;
                    }
                    messages.add(new Message(receivedJSONString));
                    repaint();
                } catch (IOException e) {
                    closeEverything(socket, bufferedReader, bufferedWriter);
                }
            }
        }).start();
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        try {
            if (!writeField.getText().isEmpty()) {
                String message = writeField.getText().trim();
                if (message.isEmpty()) {
                    return;
                }
                writeField.setText("");
                JSONObject jsonObject = new JSONObject();
                jsonObject.put("username", username);
                if (message.startsWith("/dm") || message.startsWith("/say") || message.startsWith("/whisper")) {
                    String[] split = message.split(" ", 3);
                    if (split.length != 3) {
                        JSONObject jsonObject1 = new JSONObject();
                        jsonObject1.put("username", "SERVER");
                        jsonObject1.put("message", "Invalid command. Usage: /dm <username> <message>");
                        jsonObject1.put("to", username);
                        messages.add(new Message(jsonObject1));
                        repaint();
                        return;
                    }
                    jsonObject.put("to", split[1]);
                    jsonObject.put("message", split[2]);
                } else {
                    jsonObject.put("message", message);
                    jsonObject.put("to", "everyone");
                }
                Message msgToSend = new Message(jsonObject);

                if (!msgToSend.getTo().equals(username)) {
                    bufferedWriter.write(msgToSend.getJSONString());
                    bufferedWriter.newLine();
                    bufferedWriter.flush();
                }
                synchronized (messages) {
                    messages.add(msgToSend);
                }

                repaint();

                if (msgToSend.getMessage().equalsIgnoreCase("exit")) {
                    closeEverything(socket, bufferedReader, bufferedWriter);
                }
            }
        } catch (IOException error) {
            System.out.println("Client exception: " + error.getMessage());
        }
    }

    public void repaint() {
        readField.setText("");
        Document doc = readField.getStyledDocument();

        synchronized (messages) {
            for (Message msg : messages) {
                try {
                    if (username.equals(msg.getTo()) && username.equals(msg.getUsername())) { // Self DM
                        doc.insertString(doc.getLength(), msg.getTime() + " ", timeAttr);
                        doc.insertString(doc.getLength(), "[to YOURSELF]: ", dmAttr);
                        doc.insertString(doc.getLength(), msg.getMessage() + "\n", messageAttr);
                    } else if (msg.getTo().equals(username)) { // DM to YOU
                        doc.insertString(doc.getLength(), msg.getTime() + " ", timeAttr);
                        doc.insertString(doc.getLength(), "[" + msg.getUsername() + " to YOU]: ", dmAttr);
                        doc.insertString(doc.getLength(), msg.getMessage() + "\n", messageAttr);
                    } else if (msg.getUsername().equals(username) && msg.isDM()) { // DM from YOU
                        doc.insertString(doc.getLength(), msg.getTime() + " ", timeAttr);
                        doc.insertString(doc.getLength(), "[YOU to " + msg.getTo() + "]: ", dmAttr);
                        doc.insertString(doc.getLength(), msg.getMessage() + "\n", messageAttr);
                    } else if (msg.getUsername().equals(username)) { // YOU to everyone
                        doc.insertString(doc.getLength(), msg.getTime() + " ", timeAttr);
                        doc.insertString(doc.getLength(), "[YOU]: ", dmAttr);
                        doc.insertString(doc.getLength(), msg.getMessage() + "\n", messageAttr);
                    } else { //anyone to everyone
                        doc.insertString(doc.getLength(), msg.getTime() + " ", timeAttr);
                        doc.insertString(doc.getLength(), "[" + msg.getUsername() + "]: ", usernameAttr);
                        doc.insertString(doc.getLength(), msg.getMessage() + "\n", messageAttr);
                    }
                } catch (BadLocationException | ConcurrentModificationException ignored) {
                }
            }
        }
        readField.setCaretPosition(doc.getLength());
        super.repaint();
    }

}
