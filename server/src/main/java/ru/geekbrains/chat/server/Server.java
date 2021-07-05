package ru.geekbrains.chat.server;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

public class Server {
    private List<ClientHandler> clients;
    private Object StringBuilder;
    private Connection connection;
    private Statement statement;

    public Server() {
        try {
            this.clients = new ArrayList<>();
            ServerSocket serverSocket = new ServerSocket(8189); // 1 - Создаем соединение для подключения к серверу на порт 8189
            createDB();
            System.out.println("Server is on");
            while (true) {
                Socket socket = serverSocket.accept(); // 2 - Ждем подключения клиента
                System.out.println("Client is connected");
                new ClientHandler(this, socket, connection, statement); // 3 - Передаем параметры сервера и соединения обработчику клиета
            }
        } catch (IOException e) {
            e.printStackTrace();
        } catch (SQLException throwables) {
            throwables.printStackTrace();
        }
    }

    public void createDB() throws SQLException {
        connection = DriverManager.getConnection("jdbc:sqlite:chatdb.db");
        statement = connection.createStatement();
        String sql = "create table if not exists users (\n" +
                "id integer primary key autoincrement not null,\n" +
                "login text not null,\n" +
                "password text not null,\n" +
                "username text not null\n" +
                ");";
        statement.executeUpdate(sql);
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

    public synchronized boolean checkUserName(ClientHandler c,String name) {
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
