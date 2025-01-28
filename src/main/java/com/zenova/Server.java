package com.zenova;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;
public class Server {
    private static final int PORT = 12345;
    private static List<ClientHandler> clients = new ArrayList<>();
    public static void main(String[] args) {

        try (ServerSocket serverSocket = new ServerSocket(PORT)) {
            while (true) {
                Socket clientSocket = serverSocket.accept();
                System.out.println("New client connected.");
                ClientHandler clientHandler = new ClientHandler(clientSocket);
                clients.add(clientHandler);
                new Thread(clientHandler).start();
            }
        } catch (IOException e) {
            System.out.println("Error: "+e.getMessage());
            e.printStackTrace();
        }
    }

    static class ClientHandler implements Runnable {
        private Socket socket;
        private DataInputStream in;
        private DataOutputStream out;
        public ClientHandler(Socket socket) {
            this.socket = socket;
        }
        @Override
        public void run() {
            try {
                in = new DataInputStream(socket.getInputStream());
                out = new DataOutputStream(socket.getOutputStream());
                while (true) {
                    String messageType = in.readUTF();
                    if (messageType.equals("TEXT")) {
                        String message = in.readUTF();
                        System.out.println(message);
                        broadcastMessage("TEXT", message);} else if (messageType.equals("IMAGE")) {
                        int length = in.readInt();
                        byte[] imageBytes = new byte[length];
                        in.readFully(imageBytes);
                        broadcastImage(imageBytes);
                    }
                }
            } catch (IOException e) {
                System.out.println("Client disconnected.");
                clients.remove(this);
            }
        }
        private void broadcastMessage(String type, String message) {
            System.out.println("Broadcasting message: " + type + " " + message);
            for (ClientHandler client : clients) {
                try {
                    client.out.writeUTF(type);
                    client.out.writeUTF(message);
                } catch (IOException e) {
                    System.out.println("Error: "+e.getMessage());
                    e.printStackTrace();
                }
            }
        }

        private void broadcastImage(byte[] imageBytes) {
            for (ClientHandler client : clients) {
                try {
                    client.out.writeUTF("IMAGE");
                    client.out.writeInt(imageBytes.length);
                    client.out.write(imageBytes);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }
}
