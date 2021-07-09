import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.HBox;
import javafx.stage.Stage;

import java.io.*;
import java.net.Socket;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;


public class Controller {
    private Socket socket;
    private DataInputStream in;
    private DataOutputStream out;
    private Stage stage; // сцена

    public void setStage(Stage stage) {
        this.stage = stage;
    } // запоминаем сцену

    @FXML
    TextArea chatMessages;

    @FXML
    TextField inputField, userNameField, userPasswordField;

    @FXML
    HBox authPanel, sendPanel;


    @FXML
    ListView<String> clientsListView;

    public void setAuthorized(boolean authorized) {
        sendPanel.setVisible(authorized);
        sendPanel.setManaged(authorized);
        authPanel.setVisible(!authorized);
        authPanel.setManaged(!authorized);
        clientsListView.setVisible(authorized);
        clientsListView.setManaged(authorized);
    }


    public void sendMsg(ActionEvent actionEvent) { // при нажатии кнопки отправить
        sendMsgEvent();
    }

    public void onEnter(KeyEvent keyEvent) { // при нажатии интер
        if (keyEvent.getCode() == KeyCode.ENTER) {
            sendMsgEvent();
        }
    }

    public void sendMsgEvent() {
        try {
            if (!inputField.getText().trim().isEmpty()) { // проверка на пустую строку и пробелы
                out.writeUTF(inputField.getText()); // отправляем текст в исходящий канал
            } else {
                inputField.clear();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        inputField.clear(); // очищаем инпут
        inputField.requestFocus(); // ставим фокус на инпут после отправки
    }


    public void sendCloseReq() {
        try {
            if (out != null) {
                out.writeUTF("/exit");
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void authBtnEvent(char ch) {
        connect();
        try {
            if (ch == 'a') {
                out.writeUTF("/auth " + userNameField.getText() + " " + userPasswordField.getText());
            } else {
                out.writeUTF("/register " + userNameField.getText() + " " + userPasswordField.getText());
            }
        } catch (IOException e) {
            showError("Can't send request of authorization to server ");
        }
    }

    public void tryToAuth(ActionEvent actionEvent) {
        authBtnEvent('a');
    }

    public void tryToRegister(ActionEvent actionEvent) {
        authBtnEvent('b');
    }


    public void connect() {
        if (socket != null && !socket.isClosed()) { //
            return;
        }
        try {
            socket = new Socket("localhost", 8189); // создаем новое подключение с указанием ип и орта
            in = new DataInputStream(socket.getInputStream()); // входящий поток
            out = new DataOutputStream(socket.getOutputStream()); // исходящий поток
            new Thread(() -> mainClientLogic()).start();
        } catch (IOException e) {
            showError("Can't connect to server");
        }
    }

    public void showError(String message) { // вывод сообщений
        new Alert(Alert.AlertType.ERROR, message, ButtonType.OK).showAndWait();
    }


    void showLogsFromLinkedList(File file) {
        long timeStart = System.currentTimeMillis();
        try (BufferedReader bufferedReader = new BufferedReader(new FileReader(file))) {
            List<String> readList = new LinkedList<>();
            String a;
            while ((a = bufferedReader.readLine()) != null) {
                readList.add(a);
                if (readList.size() > 100) {
                    readList.remove(0);
                }
            }
            for (int i = 0; i < 100; i++) {
                chatMessages.appendText(readList.get(i) + "\n");
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        long timeStop = System.currentTimeMillis();
        System.out.println("LinkedList time: " + (timeStop - timeStart));
    }

    void showLogsFromArrayList(File file) {
        long timeStart = System.currentTimeMillis();
        try (BufferedReader bufferedReader = new BufferedReader(new FileReader(file))) {
            List<String> lines = new ArrayList<>();
            String a;
            while ((a = bufferedReader.readLine()) != null) {
                lines.add(a);
            }
            if (lines.size() <= 100) {
                for (String line : lines)
                    chatMessages.appendText(line + "\n");
            } else {
                for (int from = lines.size() - 100; from < lines.size(); from++) {
                    chatMessages.appendText(lines.get(from) + "\n");
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        long timeStop = System.currentTimeMillis();
        System.out.println("ArrayList time: " + (timeStop - timeStart));
    }

    private void mainClientLogic() {
        File file = new File("history_" + userNameField.getText() + ".txt");
        try (BufferedWriter bufferedWriter = new BufferedWriter(new FileWriter(file, true
        ))) {
            Platform.runLater(() -> {
                showLogsFromLinkedList(file); // вывод лога на основе LinkedList (на 20% быстрее)
//              showLogsFromArrayList(file); // вывод лога (старая версия)
            });
            while (true) { // цикл авторизации
                String inputMessage = in.readUTF();
                if (inputMessage.equals("/authOk")) {
                    setAuthorized(true);
                    break;
                }
                chatMessages.appendText(inputMessage + "\n"); // добавляем
            }
            while (true) {  // цикл добавления входящих сообщений в окно чата
                String inputMessage = in.readUTF(); // считывем
                if (inputMessage.startsWith("/")) {
                    if (inputMessage.equals("/exit")) {
                        break;
                    }
                    if (inputMessage.startsWith("/clients_list ")) {  // clients_list id id id id id
                        Platform.runLater(() -> {
                            String[] tokens = inputMessage.split("\\s+");
                            clientsListView.getItems().clear();
                            for (int i = 1; i < tokens.length; i++) {
                                clientsListView.getItems().add(tokens[i]);
                            }
                        });
                    }
                    continue;
                }
                Platform.runLater(() -> {
                    stage.setTitle("Your username: " + userNameField.getText()); // Выводим юзернейм в Тайтл окна
                });
                LocalTime time = LocalTime.now();
                LocalDate date = LocalDate.now();
                chatMessages.appendText(inputMessage + "\n"); // добавляем в чат
                bufferedWriter.write("[ " + date.getDayOfMonth() + "." + date.getMonthValue() + "." + date.getYear() + " | " + time.getHour() + ":" + time.getMinute() + ":" + time.getSecond() + " ] " + inputMessage + "\n");
            }
        } catch (
                IOException e) {
            e.printStackTrace();
        } finally {
            closeConnection();
        }

    }


    private void closeConnection() {
        setAuthorized(false);
        Platform.runLater(() -> {
            stage.setTitle("LOG IN TO CHAT"); // изменение заголовка чата при отключении
        });
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

    public void listClicked(MouseEvent mouseEvent) {
        if (mouseEvent.getClickCount() == 2) {
            String selectedUser = clientsListView.getSelectionModel().getSelectedItem();
            if (!userNameField.getText().equals(selectedUser)) {
                inputField.setText("/w " + selectedUser + " ");
                inputField.requestFocus();
                inputField.selectEnd();
            }
        }
    }


}