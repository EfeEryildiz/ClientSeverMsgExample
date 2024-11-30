package org.example.clientsevermsgexample;

import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.Group;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.stage.Stage;
import java.io.*;
import java.net.*;
import java.util.ResourceBundle;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MainController implements Initializable {
    @FXML private ComboBox<String> dropdownPort;
    @FXML private TextArea resultArea;
    @FXML private TextField urlName;

    private Socket clientSocket;
    private ServerSocket serverSocket;
    private final ExecutorService executorService = Executors.newCachedThreadPool();
    private volatile boolean isServerRunning = false;
    private volatile boolean isClientRunning = false;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        dropdownPort.getItems().addAll("7", "13", "21", "23", "71", "80", "119", "161");
    }

    @FXML
    void startServer(ActionEvent event) {
        Stage serverStage = new Stage();
        TextArea serverLog = new TextArea();
        serverLog.setEditable(false);
        TextField messageField = new TextField();
        Button sendButton = new Button("Send Message");

        Group root = new Group();
        serverLog.setPrefSize(400, 300);
        messageField.setLayoutY(310);
        messageField.setPrefWidth(300);
        sendButton.setLayoutX(310);
        sendButton.setLayoutY(310);

        root.getChildren().addAll(serverLog, messageField, sendButton);
        serverStage.setScene(new Scene(root, 450, 350));
        serverStage.setTitle("Server");

        executorService.submit(() -> {
            try {
                serverSocket = new ServerSocket(6666);
                isServerRunning = true;
                Platform.runLater(() -> serverLog.appendText("Server started on port 6666\nWaiting for clients...\n"));

                while (isServerRunning) {
                    Socket socket = serverSocket.accept();
                    handleClientConnection(socket, serverLog, messageField, sendButton);
                }
            } catch (IOException e) {
                Platform.runLater(() -> serverLog.appendText("Server error: " + e.getMessage() + "\n"));
            }
        });

        serverStage.setOnCloseRequest(e -> {
            isServerRunning = false;
            try {
                if (serverSocket != null && !serverSocket.isClosed()) {
                    serverSocket.close();
                }
            } catch (IOException ex) {
                ex.printStackTrace();
            }
        });

        serverStage.show();
    }

    private void handleClientConnection(Socket socket, TextArea log, TextField messageField, Button sendButton) {
        try {
            DataInputStream input = new DataInputStream(socket.getInputStream());
            DataOutputStream output = new DataOutputStream(socket.getOutputStream());

            Platform.runLater(() -> log.appendText("Client connected!\n"));

            sendButton.setOnAction(e -> {
                try {
                    String message = messageField.getText();
                    if (!message.isEmpty()) {
                        output.writeUTF("Server: " + message);
                        Platform.runLater(() -> {
                            log.appendText("Me: " + message + "\n");
                            messageField.clear();
                        });
                    }
                } catch (IOException ex) {
                    Platform.runLater(() -> log.appendText("Error sending message: " + ex.getMessage() + "\n"));
                }
            });

            new Thread(() -> {
                try {
                    while (!socket.isClosed()) {
                        String message = input.readUTF();
                        Platform.runLater(() -> log.appendText("Client: " + message + "\n"));
                    }
                } catch (IOException e) {
                    Platform.runLater(() -> log.appendText("Client disconnected\n"));
                }
            }).start();

        } catch (IOException e) {
            Platform.runLater(() -> log.appendText("Error handling client: " + e.getMessage() + "\n"));
        }
    }

    @FXML
    void startClient(ActionEvent event) {
        Stage clientStage = new Stage();
        TextArea clientLog = new TextArea();
        clientLog.setEditable(false);
        TextField messageField = new TextField();
        Button sendButton = new Button("Send Message");
        Button connectButton = new Button("Connect to Server");

        Group root = new Group();
        clientLog.setPrefSize(400, 300);
        messageField.setLayoutY(310);
        messageField.setPrefWidth(300);
        sendButton.setLayoutX(310);
        sendButton.setLayoutY(310);
        connectButton.setLayoutY(340);

        root.getChildren().addAll(clientLog, messageField, sendButton, connectButton);
        clientStage.setScene(new Scene(root, 450, 380));
        clientStage.setTitle("Client");

        connectButton.setOnAction(e -> {
            connectButton.setDisable(true);
            connectToServer(clientLog, messageField, sendButton);
        });

        clientStage.show();
    }

    private void connectToServer(TextArea log, TextField messageField, Button sendButton) {
        executorService.submit(() -> {
            try {
                clientSocket = new Socket("localhost", 6666);
                DataInputStream input = new DataInputStream(clientSocket.getInputStream());
                DataOutputStream output = new DataOutputStream(clientSocket.getOutputStream());
                isClientRunning = true;

                Platform.runLater(() -> {
                    log.appendText("Connected to server!\n");
                    sendButton.setOnAction(event -> {
                        try {
                            String message = messageField.getText();
                            if (!message.isEmpty()) {
                                output.writeUTF(message);
                                Platform.runLater(() -> {
                                    log.appendText("Me: " + message + "\n");
                                    messageField.clear();
                                });
                            }
                        } catch (IOException ex) {
                            log.appendText("Error sending message: " + ex.getMessage() + "\n");
                        }
                    });
                });

                while (isClientRunning && !clientSocket.isClosed()) {
                    String message = input.readUTF();
                    Platform.runLater(() -> log.appendText(message + "\n"));
                }
            } catch (IOException e) {
                Platform.runLater(() -> log.appendText("Connection error: " + e.getMessage() + "\n"));
            }
        });
    }

    @FXML
    void testPort(ActionEvent event) {
        String host = urlName.getText();
        if (host.isEmpty() || dropdownPort.getValue() == null) {
            resultArea.appendText("Please enter a server and select a port\n");
            return;
        }

        int port = Integer.parseInt(dropdownPort.getValue());
        executorService.submit(() -> {
            try {
                Socket testSocket = new Socket(host, port);
                Platform.runLater(() -> resultArea.appendText(host + " listening on port " + port + "\n"));
                testSocket.close();
            } catch (UnknownHostException e) {
                Platform.runLater(() -> resultArea.appendText("Unknown host: " + host + "\n"));
            } catch (IOException e) {
                Platform.runLater(() -> resultArea.appendText(host + " not listening on port " + port + "\n"));
            }
        });
    }

    @FXML
    void clearBtn(ActionEvent event) {
        resultArea.clear();
        urlName.clear();
    }
}