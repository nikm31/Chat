package ru.geekbrains.chat.server;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.sql.*;

public class ClientHandler {
    private Socket socket;
    private DataInputStream in;
    private DataOutputStream out;
    private Server server;
    private String username;
    private Connection connection;
    private Statement statement;

    public String getUsername() {
        return username;
    }

    public ClientHandler(Server server, Socket socket, Connection connection, Statement statement) {
        try {
            this.statement = statement;
            this.connection = connection;
            this.server = server;
            this.socket = socket;
            this.in = new DataInputStream(socket.getInputStream()); // входящие поток
            this.out = new DataOutputStream(socket.getOutputStream());  // исходящий поток
            new Thread(() -> mainLogic()).start(); // 4 - создаем поток для общения с клиентом
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void sendMessage(String message) {
        try {
            out.writeUTF(message); // отправляем сообщение клиенту
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void mainLogic() {
        try {
            while (!consumeAuthorizeMessages(in.readUTF())) ;  // цикл авторизации
            while (consumeRegularMessages(in.readUTF())) ;    // цикл рассылки сообщений
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            System.out.println("Client " + username + " is now Offline");
            server.unSubscribe(this, username); // если клиент вышел
            closeConnection();
        }
    }

    private boolean consumeRegularMessages(String inputMessage) {
        if (inputMessage.startsWith("/")) { // если сообщение начинается с /, то пропускаем
            if (inputMessage.equals("/exit")) {
                sendMessage("/exit");
                return false;
            }
            if (inputMessage.startsWith("/w ")) { // /w id hello word
                String[] tokens = inputMessage.split("\\s+", 3);
                if (!tokens[1].equals(username)) {
                    server.sendPersonalMessage(this, tokens[1], tokens[2]);
                }
            }
            if (inputMessage.startsWith("/change ")) {
                String[] tokens = inputMessage.split("\\s+");
                String login = tokens[1];
                String password = tokens[2];
                String newUserName = tokens[3];
                try (PreparedStatement preparedStatement = connection.prepareStatement("update users set username = ? where login = ? and password = ? ")) {
                    preparedStatement.setString(1, newUserName);
                    preparedStatement.setString(2, login);
                    preparedStatement.setString(3, password);
                    preparedStatement.execute();
                    server.broadcastMessage("Пользователь" + getUsername() + " сменил ник на: " + newUserName);
                } catch (SQLException throwables) {
                    throwables.printStackTrace();
                }
            }
            return true;
        }
        server.broadcastMessage(username + ": " + inputMessage); // вещаем сообщение клиентам
        return true;
    }


    private void addUserToDB(String message) {
        String[] tokens = message.split("\\s+");
        try (PreparedStatement preparedStatement = connection.prepareStatement("select * from users where login = ? ")) {
            preparedStatement.setString(1, tokens[1]);
            ResultSet rs = preparedStatement.executeQuery();
            if (!rs.next()) {
                try (PreparedStatement preparedStatement2 = connection.prepareStatement("insert into users (login, password, username) values (? ,? ,?)")) {
                    preparedStatement2.setString(1, tokens[1]);
                    preparedStatement2.setString(2, tokens[2]);
                    preparedStatement2.setString(3, tokens[1]);
                    preparedStatement2.execute();
                    sendMessage("Your username: " + tokens[1] + " is registered");
                }
            } else {
                sendMessage("login: " + tokens[1] + " is exist");
            }
        } catch (SQLException throwables) {
            throwables.printStackTrace();
        }
    }

    private boolean consumeAuthorizeMessages(String message) { //////////////////////// болеан?
        if (message.startsWith("/register ")) {
            addUserToDB(message);
            return false;
        }
        if (message.startsWith("/auth ")) { // проверка ввода логина
            String[] tokens = message.split("\\s+"); // разбивка строки аунтефикации
            if (tokens.length == 1) {
                sendMessage("Your did not enter password");
                return false;
            }
            if (tokens.length > 3) {
                sendMessage("Your name is above 2 words");
                return false;
            }
            String login = tokens[1]; // вычисляем имя
            String password = tokens[2]; // вычисляем пароль
            try (PreparedStatement preparedStatement = connection.prepareStatement("select username from users where login = ? and password = ?")) {
                preparedStatement.setString(1, login);
                preparedStatement.setString(2, password);
                ResultSet rs = preparedStatement.executeQuery();
                if (!rs.next()) {
                    sendMessage("Вы ввели не верный логин или пароль. Попробуйте снова");
                } else {
                    login = rs.getString("username");
                    if (!server.checkUserName(this, login)) { // проверка на уникальность юзера
                        return true;
                    }
                    username = login;
                    sendMessage("/authOk"); // системное сообщение о успешной авторизации
                    sendMessage("/userName " + username); // системное сообщение о успешной авторизации
                    server.subscribe(this, username); // подписываем на рассылку
                    return true;
                }
            } catch (SQLException ex) {
                ex.printStackTrace();
            }
            return false;
        } else {
            sendMessage("SERVER: You need to login");
            return false;
        }
    }

    private void closeConnection() {
        if (statement != null) {
            try {
                statement.close();
            } catch (SQLException throwables) {
                throwables.printStackTrace();
            }
        }
        if (connection != null) {
            try {
                connection.close();
            } catch (SQLException throwables) {
                throwables.printStackTrace();
            }
        }
        if (in != null) {
            try {
                in.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        if (out != null) {
            try {
                out.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        if (socket != null) {
            try {
                socket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}