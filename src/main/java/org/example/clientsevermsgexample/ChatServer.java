package org.example.clientsevermsgexample;

import java.io.*;
import java.net.*;
import java.util.*;

public class ChatServer {
    private ServerSocket serverSocket;
    private List<ClientHandler> clients = new ArrayList<>();
    private boolean isRunning = true;

    public void start(int port) {
        try {
            serverSocket = new ServerSocket(port);
            while (isRunning) {
                Socket clientSocket = serverSocket.accept();
                ClientHandler client = new ClientHandler(clientSocket);
                clients.add(client);
                new Thread(client).start();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void broadcast(String message) {
        for (ClientHandler client : clients) {
            client.sendMessage(message);
        }
    }

    private class ClientHandler implements Runnable {
        private Socket socket;
        private DataInputStream input;
        private DataOutputStream output;

        public ClientHandler(Socket socket) {
            this.socket = socket;
            try {
                input = new DataInputStream(socket.getInputStream());
                output = new DataOutputStream(socket.getOutputStream());
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        @Override
        public void run() {
            try {
                while (isRunning) {
                    String message = input.readUTF();
                    broadcast(message);
                }
            } catch (IOException e) {
                clients.remove(this);
            }
        }

        public void sendMessage(String message) {
            try {
                output.writeUTF(message);
            } catch (IOException e) {
                clients.remove(this);
            }
        }
    }
}