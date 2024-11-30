package org.example.clientsevermsgexample;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.control.Button;

import java.io.*;
import java.net.Socket;

public class ChatController {
    @FXML private TextArea chatArea;
    @FXML private TextField messageField;
    @FXML private Button sendButton;

    private Socket socket;
    private DataInputStream input;
    private DataOutputStream output;
    private boolean isRunning = true;

    public void initialize(String host, int port, String username) {
        try {
            socket = new Socket(host, port);
            input = new DataInputStream(socket.getInputStream());
            output = new DataOutputStream(socket.getOutputStream());

            // Start message receiving thread
            new Thread(this::receiveMessages).start();

            sendButton.setOnAction(e -> sendMessage());
            messageField.setOnAction(e -> sendMessage());

        } catch (IOException e) {
            chatArea.appendText("Connection error: " + e.getMessage() + "\n");
        }
    }

    private void sendMessage() {
        try {
            String message = messageField.getText();
            if (!message.isEmpty()) {
                output.writeUTF(message);
                messageField.clear();
            }
        } catch (IOException e) {
            chatArea.appendText("Error sending message: " + e.getMessage() + "\n");
        }
    }

    private void receiveMessages() {
        while (isRunning) {
            try {
                String message = input.readUTF();
                Platform.runLater(() -> chatArea.appendText(message + "\n"));
            } catch (IOException e) {
                if (isRunning) {
                    Platform.runLater(() -> chatArea.appendText("Connection lost\n"));
                    break;
                }
            }
        }
    }

    public void shutdown() {
        isRunning = false;
        try {
            if (socket != null) socket.close();
            if (input != null) input.close();
            if (output != null) output.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
