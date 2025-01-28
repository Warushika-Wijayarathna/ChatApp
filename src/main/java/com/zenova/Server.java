package com.zenova;

import java.io.*;
import java.net.*;
import java.util.*;

public class Server {
    private static final int PORT = 12345;
    private static Set<Socket> clientSockets = Collections.synchronizedSet(new HashSet<>());

    public static void main(String[] args) {
        try (ServerSocket serverSocket = new ServerSocket(PORT)) {
            System.out.println("Server started on port " + PORT);

            while (true) {
                Socket clientSocket = serverSocket.accept();
                clientSockets.add(clientSocket);
                new Thread(new ClientHandler(clientSocket)).start();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static class ClientHandler implements Runnable {
        private Socket socket;
        private DataInputStream in;
        private DataOutputStream out;

        public ClientHandler(Socket socket) {
            this.socket = socket;
            try {
                in = new DataInputStream(socket.getInputStream());
                out = new DataOutputStream(socket.getOutputStream());
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        @Override
        public void run() {
            try {
                while (true) {
                    String messageType = in.readUTF();
                    if (messageType.equals("TEXT")) {
                        String message = in.readUTF();
                        broadcastMessage("TEXT", message);
                    } else if (messageType.equals("IMAGE")) {
                        int length = in.readInt();
                        byte[] imageBytes = new byte[length];
                        in.readFully(imageBytes);
                        broadcastImage(imageBytes);
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                try {
                    socket.close();
                    clientSockets.remove(socket);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }

        private void broadcastMessage(String type, String message) {
            synchronized (clientSockets) {
                for (Socket clientSocket : clientSockets) {
                    try {
                        DataOutputStream clientOut = new DataOutputStream(clientSocket.getOutputStream());
                        clientOut.writeUTF(type);
                        clientOut.writeUTF(message);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
        }

        private void broadcastImage(byte[] imageBytes) {
            synchronized (clientSockets) {
                for (Socket clientSocket : clientSockets) {
                    try {
                        DataOutputStream clientOut = new DataOutputStream(clientSocket.getOutputStream());
                        clientOut.writeUTF("IMAGE");
                        clientOut.writeInt(imageBytes.length);
                        clientOut.write(imageBytes);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
        }
    }
}
