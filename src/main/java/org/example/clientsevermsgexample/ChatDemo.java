package org.example.clientsevermsgexample;

import javafx.application.Platform;
import javafx.scene.Group;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.stage.Stage;

import java.io.*;
import java.net.*;
import java.util.concurrent.*;

public class ChatDemo {
    private Socket socket;
    private ServerSocket serverSocket;
    private DataInputStream inputStream;
    private DataOutputStream outputStream;
    private TextArea messageDisplay;
    private TextField inputField;
    private volatile boolean isRunning = true;
    private ExecutorService executorService = Executors.newCachedThreadPool();

    public void createChatWindow(boolean isServer) {
        Stage stage = new Stage();
        Group root = new Group();

        // Create UI components
        messageDisplay = new TextArea();
        messageDisplay.setLayoutX(10);
        messageDisplay.setLayoutY(10);
        messageDisplay.setPrefSize(400, 300);
        messageDisplay.setEditable(false);
        messageDisplay.setWrapText(true);

        inputField = new TextField();
        inputField.setLayoutX(10);
        inputField.setLayoutY(320);
        inputField.setPrefWidth(300);

        Button sendButton = new Button(isServer ? "Reply" : "Send");
        sendButton.setLayoutX(320);
        sendButton.setLayoutY(320);

        Button connectButton = new Button("Connect");
        connectButton.setLayoutX(320);
        connectButton.setLayoutY(360);

        // Add event handlers
        sendButton.setOnAction(e -> sendMessage());
        inputField.setOnAction(e -> sendMessage());
        connectButton.setOnAction(e -> {
            if (isServer) {
                startServer();
            } else {
                connectToServer();
            }
            connectButton.setDisable(true);
        });

        // Setup scene
        root.getChildren().addAll(messageDisplay, inputField, sendButton, connectButton);
        Scene scene = new Scene(root, 450, 400);
        stage.setTitle(isServer ? "Chat Server" : "Chat Client");
        stage.setScene(scene);

        // Cleanup on window close
        stage.setOnCloseRequest(e -> {
            isRunning = false;
            cleanup();
        });

        stage.show();
    }

    private void startServer() {
        executorService.submit(() -> {
            try {
                updateDisplay("Starting server...");
                serverSocket = new ServerSocket(6666);
                updateDisplay("Waiting for client connection...");
                socket = serverSocket.accept();
                setupStreams();
                updateDisplay("Client connected!");
            } catch (IOException e) {
                updateDisplay("Server error: " + e.getMessage());
            }
        });
    }

    private void connectToServer() {
        executorService.submit(() -> {
            try {
                updateDisplay("Connecting to server...");
                socket = new Socket("localhost", 6666);
                setupStreams();
                updateDisplay("Connected to server!");
            } catch (IOException e) {
                updateDisplay("Connection error: " + e.getMessage());
            }
        });
    }

    private void setupStreams() throws IOException {
        inputStream = new DataInputStream(socket.getInputStream());
        outputStream = new DataOutputStream(socket.getOutputStream());
        startMessageReceiver();
    }

    private void startMessageReceiver() {
        executorService.submit(() -> {
            while (isRunning && socket != null && !socket.isClosed()) {
                try {
                    String message = inputStream.readUTF();
                    updateDisplay("Received: " + message);
                } catch (IOException e) {
                    if (isRunning) {
                        updateDisplay("Connection lost: " + e.getMessage());
                    }
                    break;
                }
            }
        });
    }

    private void sendMessage() {
        if (socket == null || socket.isClosed() || outputStream == null) {
            updateDisplay("Not connected!");
            return;
        }

        String message = inputField.getText().trim();
        if (message.isEmpty()) {
            return;
        }

        executorService.submit(() -> {
            try {
                outputStream.writeUTF(message);
                Platform.runLater(() -> {
                    updateDisplay("Sent: " + message);
                    inputField.clear();
                });
            } catch (IOException e) {
                updateDisplay("Failed to send message: " + e.getMessage());
            }
        });
    }

    private void updateDisplay(String message) {
        Platform.runLater(() -> {
            messageDisplay.appendText(message + "\n");
            messageDisplay.setScrollTop(Double.MAX_VALUE);
        });
    }

    private void cleanup() {
        try {
            isRunning = false;
            if (inputStream != null) inputStream.close();
            if (outputStream != null) outputStream.close();
            if (socket != null) socket.close();
            if (serverSocket != null) serverSocket.close();
            executorService.shutdown();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}