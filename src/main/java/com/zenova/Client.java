package com.zenova;

import javafx.application.Application;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.*;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import java.io.*;
import java.net.Socket;

public class Client extends Application {
    private VBox msgBox;
    private DataOutputStream out;


    @Override
    public void start(Stage primaryStage) {
        primaryStage.setTitle("Chat Application");
        BorderPane root = new BorderPane();

        msgBox = new VBox(10);
        msgBox.setPadding(new Insets(10));

        ScrollPane scrollPane = new ScrollPane(msgBox);
        scrollPane.setFitToWidth(true);

        TextField messageField = new TextField();
        messageField.setPromptText("Enter your message...");
        messageField.setPrefHeight(40);

        Button sendButton = new Button("Send");
        Button imageButton = new Button("send Image");
        HBox inputBox = new HBox(10, messageField, sendButton, imageButton);

        inputBox.setPadding(new Insets(10));
        inputBox.setHgrow(messageField, Priority.ALWAYS);
        root.setCenter(scrollPane);
        root.setBottom(inputBox);

        Scene scene = new Scene(root, 500, 600);
        primaryStage.setScene(scene);primaryStage.show();

        new Thread(() -> connectServer()).start();

        sendButton.setOnAction(e -> sendMsgs(messageField.getText()));
        messageField.setOnKeyPressed(e -> {
            if (e.getCode() == KeyCode.ENTER) {
                sendMsgs(messageField.getText());
            }
        });
        imageButton.setOnAction(e -> sendImg(primaryStage));
    }

    private void connectServer() {
        try {
            Socket socket = new Socket("localhost", 12345);
            out = new DataOutputStream(socket.getOutputStream());
            DataInputStream in = new DataInputStream(socket.getInputStream());

            while (true) {
                String messageType = in.readUTF();
                if (messageType.equals("TEXT")) {
                    String message = in.readUTF();
                    addMsgToUI(message);
                } else if (messageType.equals("IMAGE")) {
                    int length = in.readInt();
                    byte[] imageBytes = new byte[length];
                    in.readFully(imageBytes);
                    addImageToUI(imageBytes);
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void sendImg(Stage stage) {
        FileChooser fileChooser = new FileChooser();
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Image Files",
                "*.png", "*.jpg", "*.jpeg"));
        File file = fileChooser.showOpenDialog(stage);
        if (file != null) {
            try {
                byte[] imageBytes = readImage(file);
                out.writeUTF("IMAGE");
                out.writeInt(imageBytes.length);
                out.write(imageBytes);
                addImageToUI(imageBytes);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private byte[] readImage(File file) throws IOException {
        FileInputStream fis = new FileInputStream(file);
        byte[] imageBytes = new byte[(int) file.length()];
        fis.read(imageBytes);
        fis.close();
        return imageBytes;
    }

    private void addMsgToUI(String message) {
        Label label = new Label(message);
        label.setWrapText(true);
        msgBox.getChildren().add(label);
    }

    private void addImageToUI(byte[] imageBytes) {
        Image image = new Image(new ByteArrayInputStream(imageBytes));
        ImageView imageView = new ImageView(image);
        imageView.setFitWidth(200);
        imageView.setPreserveRatio(true);
        msgBox.getChildren().add(imageView);
    }

    private void sendMsgs(String message) {
        if (message.isEmpty()) {
            try{
                out.writeUTF("TEXT");
                out.writeUTF(message);
                addMsgToUI(message);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }

    public static void main(String[] args) {
        launch(args);
    }

}
