package org.example.clientsevermsgexample;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;
import javafx.scene.text.Text;
import javafx.scene.text.TextFlow;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class ChatController {
    @FXML private VBox messageArea;
    @FXML private TextField messageInput;
    @FXML private Label statusLabel;

    private PrintWriter writer;
    private BufferedReader reader;
    private Socket socket;
    private ServerSocket serverSocket;
    private boolean isServer;
    private static final int PORT = 6666;
    private volatile boolean running = true;

    public void initialize() {
        messageArea = new VBox(10); // 10px spacing between messages
        messageArea.setStyle("-fx-padding: 10;");
        messageInput = new TextField();
        statusLabel = new Label();
    }

    public void startAsServer() {
        isServer = true;
        new Thread(() -> {
            try {
                serverSocket = new ServerSocket(PORT);
                updateStatus("Server started. Waiting for connection...");

                socket = serverSocket.accept();
                setupCommunication();
                updateStatus("Client connected!");

                startMessageListener();
            } catch (IOException e) {
                updateStatus("Server error: " + e.getMessage());
            }
        }).start();
    }

    public void startAsClient() {
        isServer = false;
        new Thread(() -> {
            try {
                socket = new Socket("localhost", PORT);
                setupCommunication();
                updateStatus("Connected to server!");

                startMessageListener();
            } catch (IOException e) {
                updateStatus("Client error: " + e.getMessage());
            }
        }).start();
    }

    private void setupCommunication() throws IOException {
        writer = new PrintWriter(socket.getOutputStream(), true);
        reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
    }

    private void startMessageListener() {
        new Thread(() -> {
            try {
                String message;
                while (running && (message = reader.readLine()) != null) {
                    String finalMessage = message;
                    Platform.runLater(() -> displayMessage(finalMessage, false));
                }
            } catch (IOException e) {
                if (running) {
                    Platform.runLater(() -> updateStatus("Connection lost: " + e.getMessage()));
                }
            }
        }).start();
    }

    @FXML
    private void sendMessage() {
        String message = messageInput.getText().trim();
        if (!message.isEmpty()) {
            writer.println(message);
            displayMessage(message, true);
            messageInput.clear();
        }
    }

    private void displayMessage(String message, boolean isSent) {
        TextFlow messageFlow = new TextFlow();

        // Add timestamp
        Text timeStamp = new Text("[" +
                LocalDateTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss")) + "] ");
        timeStamp.setStyle("-fx-fill: gray;");

        // Add sender indicator
        Text sender = new Text((isSent ? "You: " : "Other: "));
        sender.setStyle("-fx-font-weight: bold; -fx-fill: " +
                (isSent ? "blue" : "green") + ";");

        // Add message content
        Text content = new Text(message + "\n");

        messageFlow.getChildren().addAll(timeStamp, sender, content);
        messageArea.getChildren().add(messageFlow);
    }

    private void updateStatus(String status) {
        Platform.runLater(() -> statusLabel.setText(status));
    }

    public void shutdown() {
        running = false;
        try {
            if (writer != null) writer.close();
            if (reader != null) reader.close();
            if (socket != null) socket.close();
            if (serverSocket != null) serverSocket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}