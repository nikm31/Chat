package ru.geekbrains.chat.server;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;


public class Server {
    private List<ClientHandler> clients;
    private Connection connection;
    private Statement statement;
    private ExecutorService mainCachedThreads;
    private static final Logger LOGGER = LogManager.getLogger(Server.class.getName());


    public Server() {
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
            LOGGER.error(throwables.getMessage());
        }
        try {
            this.clients = new ArrayList<>();
            ServerSocket serverSocket = new ServerSocket(8189); // 1 - Создаем соединение для подключения к серверу на порт 8189
            System.out.println("Server is on");
            mainCachedThreads = Executors.newCachedThreadPool();
            while (true) {
                try {
                    Socket socket = serverSocket.accept(); // 2 - Ждем подключения клиента
                            LOGGER.info("New client is connected to Server");
                            new ClientHandler(Server.this, socket, connection, statement); // 3 - Передаем параметры сервера и соединения обработчику клиета
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
            LOGGER.error(e.getMessage());
        } finally {
            mainCachedThreads.shutdown();
            LOGGER.info("Сервер успешно завершил работу");
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
        LOGGER.info("User is logged out: " + name);
    }

    public synchronized boolean checkUserName(ClientHandler c, String name) {
        for (ClientHandler client : clients) {
            if (client.getUsername().equalsIgnoreCase(name)) {
                c.sendMessage("Username is exist");
                return false;
            }
        }
        c.sendMessage("You are lodged in");
        LOGGER.info("User is connected to chat: " + name);
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
