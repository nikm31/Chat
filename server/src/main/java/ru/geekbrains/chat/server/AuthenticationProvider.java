package ru.geekbrains.chat.server;

public interface AuthenticationProvider {
    String getUsernameByLoginAndPassword(String login, String password);
}