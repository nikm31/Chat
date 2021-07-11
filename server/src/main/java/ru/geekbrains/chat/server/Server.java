package ru.geekbrains.chat.server;

import com.sun.xml.internal.ws.api.model.wsdl.WSDLOutput;
import javafx.fxml.Initializable;
import org.w3c.dom.ls.LSOutput;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.URL;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.ResourceBundle;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Server implements Initializable {
    private List<ClientHandler> clients;
    private Object StringBuilder;
    private Connection connection;
    private Statement statement;
    private ExecutorService mainCachedThreads;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        try {
            connection = DriverManager.getConnection("jdbc:sqlite:chatdb.db");
            statement = connection.createStatement();
            String sql = "create table if not exists users (\n" +
                    "id integer primary key autoincrement not null,\n" +
                    "login text not null,\n" +
                    "password text not null,\n" +
                    "username text not null\n" +
                    ");";
            statement.executeUpdate(sql);
        } catch (SQLException throwables) {
            throwables.printStackTrace();
        }
    }

    public Server() {
        try {
            this.clients = new ArrayList<>();
            ServerSocket serverSocket = new ServerSocket(8189); // 1 - Создаем соединение для подключения к серверу на порт 8189
            System.out.println("Server is on");
            mainCachedThreads = Executors.newCachedThreadPool();
            while (true) {
                try {
                    Socket socket = serverSocket.accept(); // 2 - Ждем подключения клиента
                            System.out.println("Client is connected");
                            new ClientHandler(Server.this, socket, connection, statement); // 3 - Передаем параметры сервера и соединения обработчику клиета
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            mainCachedThreads.shutdown();
        }
    }

    public ExecutorService getMainCachedThreads() {
        return mainCachedThreads;
    }

    public synchronized void broadcastMessage(String message) {
        for (ClientHandler c : clients) { // проъодимся по массиву клиентов
            c.sendMessage(message);
        }
    }

    public synchronized void broadcastClientsList() {
        StringBuilder builder = new StringBuilder(clients.size() * 10);
        builder.append("/clients_list ");
        for (ClientHandler client : clients) {
            builder.append(client.getUsername()).append(" ");
        }
        String clientsListStr = builder.toString();
        broadcastMessage(clientsListStr);
    }

    public synchronized void subscribe(ClientHandler c, String name) {
        clients.add(c); // доюавляем клиента
        broadcastMessage("Welcome new user: " + name);
        broadcastClientsList();
    }

    public synchronized void unSubscribe(ClientHandler c, String name) {
        clients.remove(c); // удаляем клиента
        broadcastMessage("User is logged out: " + name);
        broadcastClientsList();
    }

    public synchronized boolean checkUserName(ClientHandler c, String name) {
        for (ClientHandler client : clients) {
            if (client.getUsername().equalsIgnoreCase(name)) {
                c.sendMessage("Username is exist");
                return false;
            }
        }
        c.sendMessage("You are lodged in");
        return true;
    }

    public synchronized void sendPersonalMessage(ClientHandler sender, String receiverUsername, String message) {
        for (ClientHandler c : clients) {
            if (c.getUsername().equals(receiverUsername)) {
                c.sendMessage("from" + sender.getUsername() + ": " + message);
                sender.sendMessage("to User" + receiverUsername + ": " + message);
                return;
            }

        }
        sender.sendMessage("User is offline");
    }

}
