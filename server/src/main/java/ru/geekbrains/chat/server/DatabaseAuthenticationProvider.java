package ru.geekbrains.chat.server;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class DatabaseAuthenticationProvider implements AuthenticationProvider {
    private class UserInfo {
        private String login;
        private String password;
        private String username;

        public UserInfo(String login, String password, String username) {
            this.login = login;
            this.password = password;
            this.username = username;
        }
    }

    private List<UserInfo> users;

    public DatabaseAuthenticationProvider() {
        this.users = new ArrayList<>(Arrays.asList(
                new UserInfo("user@gmail.com", "1234", "user"),
                new UserInfo("bob@gmail.com", "1234", "bob")
        ));
    }

    @Override
    public String getUsernameByLoginAndPassword(String login, String password) {
        for (UserInfo u : users) {
            if (u.login.equals(login) && u.password.equals(password)) {
                return u.username;
            }
        }
        return null;
    }
}
