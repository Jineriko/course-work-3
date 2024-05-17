package com.itmo.server;

import com.itmo.common.ConnectionService;
import com.itmo.common.Message;
import com.itmo.common.TxtFile;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public class Server {
    private int port;
    private final List<TxtFile> collectionTxtFiles = new CopyOnWriteArrayList<>();
    private final List<ConnectionService> connectionHandlers = new CopyOnWriteArrayList<>();
    private int counter = 0; // счетчик, который пригодится для хранения одинаковых имен файлов в коллекции (если такие будут)

    public Server(int port) {
        this.port = port;
    }

    public void startServer() {
        try (ServerSocket serverSocket = new ServerSocket(port)) {
            while (true) {
                try {
                    serverSocket.setReuseAddress(true);
                    Socket socket = serverSocket.accept();
                    ConnectionService connectionHandler = new ConnectionService(socket);

                    connectionHandlers.add(connectionHandler);
                    new ThreadForClient(connectionHandler).start(); // запуск потока
                } catch (Exception e) {
                    System.out.println("Проблема с установкой нового соединения");
                }
            }
        } catch (IOException e) {
            System.out.println("Ошибка запуска сервера");
            throw new RuntimeException(e);
        }
    }

    private class ThreadForClient extends Thread {
        private final ConnectionService connectionHandler;

        public ThreadForClient(ConnectionService connectionHandler) {
            this.connectionHandler = connectionHandler;
        }

        @Override
        public void run() {
            while (true) {
                Object inputObject = null;
                try {
                    inputObject = connectionHandler.read();

                    if (inputObject instanceof Message) { // условие, которое определяет это сообщение или файл
                        messageProcessing((Message) inputObject);
                    } else if (inputObject instanceof TxtFile) {
                        fileProcessing((TxtFile) inputObject);
                    } else if (inputObject instanceof Integer) {
                        if ((int) inputObject == 3)
                            requestProcessing();
                    } else if (inputObject instanceof String) {
                        sendingFile((String) inputObject);
                    } else System.out.println("Неизвестная команда");

                } catch (IOException e) {
                    System.out.println("Потеряно соединение с клиентом ");
                    connectionHandlers.remove(connectionHandler);
                    return;
                }
            }
        }

        private void requestProcessing() { // приватный метод обработки входящих сообщений (String'ов)
            StringBuilder sb = new StringBuilder("");
            for (TxtFile collectionTxtFile : collectionTxtFiles) {
                sb.append(collectionTxtFile.getFileName() + " : " + collectionTxtFile.getDescription() + "\n");
            }
            if (sb.toString().equals(""))
                sb.append("На сервере нет доступных фалов");
            connectionHandler.sendString(sb.toString());
        }

        private void messageProcessing(Message inputMessage) { // приватный метод обработки входящих сообщений
            Message fromClient = inputMessage;

            System.out.println(fromClient.getText()); // вывод сообщения от пользователя на сервере
            Message message = new Message(fromClient.getSender());
            message.setText("Пользователь " + message.getSender() + " прислал сообщение: " + fromClient.getText());

            sendingMessages(message);
        }

        private void fileProcessing(TxtFile inputFile) { // приватный метод обработки входящих файлов
            if (inputFile == null) {
                System.out.println("Файл не может быть null ссылкой");
                return;
            }
            TxtFile file = inputFile;

            if (file.getAbsolutePath().length() > 100) { // проверки на ограничение файла
                System.out.println("Название файла превышает допустимый размер в 100 символов.\n " +
                        "Невозможно загрузить файл на сервер");
                return;
            }
            long fileSize = file.length() / 1_048_576; // размер файла в Мб
            if (fileSize > 10) { // проверка на ограничение файла по Мб
                System.out.println("Размер файла больше 10 Мб.\n " +
                        "Невозможно загрузить файл на сервер");
                return;
            }

            for (TxtFile txtFile : collectionTxtFiles) {
                if (txtFile.getFileName().equals(inputFile.getFileName())) {
                    counter++;
                    String subStr = inputFile.getFileName().substring(0, inputFile.getFileName().length() - 4); // удаление последних 4 символов из имени файла
                    inputFile.setFileName(subStr + "(" + counter + ").txt"); // переименовываем файл если в коллекции будут файлы с таким же именем
                }
            }
            collectionTxtFiles.add(inputFile); // добавление файла в архив
            connectionHandler.writeTxtFile("server_" + inputFile.getFileName(), inputFile); // запись полученного от клиента файла на сервере

            System.out.println("На сервер был загружен новый файл " + inputFile.getFileName() + " пользователем: " + inputFile.getSender());
            sendingDescriptionFile(inputFile);
        }

        private synchronized void sendingMessages(Message message) { // рассылка сообщений
            connectionHandler.setFlagMessage(true);
            for (ConnectionService handler : connectionHandlers) {
                if (!handler.isFlagMessage()) {
                    try {
                        handler.sendMessage(message);
                    } catch (Exception e) {
                        System.out.println("Ошибка отправки сообщения с сервера, соединение потеряно");
                        connectionHandlers.remove(connectionHandler);
                    }
                }
            }
            connectionHandler.setFlagMessage(false);
        }

        private synchronized void sendingDescriptionFile(TxtFile file) { // рассылка сообщений
            connectionHandler.setFlagDescriptionFile(true);
            for (ConnectionService handler : connectionHandlers) {
                if (!handler.isFlagDescriptionFile()) {
                    try {
                        handler.sendDescriptionFile(file);
                    } catch (Exception e) {
                        System.out.println("Ошибка отправки описания файла с сервера, соединение потеряно");
                        connectionHandlers.remove(connectionHandler);
                    }
                }
            }
            connectionHandler.setFlagDescriptionFile(false);
        }

        public synchronized void sendingFile(String fileName) { // запрос файла
            TxtFile chosenFile = null;
            for (TxtFile txtFile : collectionTxtFiles) {
                if (txtFile.getFileName().equals(fileName)) {
                    chosenFile = txtFile;
                    continue;
                }
            }
            if (chosenFile == null) {
                try {
                    connectionHandler.sendString("Такого файла на сервере нет");
                } catch (Exception e) {
                    System.out.println("Ошибка отправки сообщения с сервер");
                }
            } else {
                try {
                    connectionHandler.sendFile(chosenFile);
                } catch (Exception e) {
                    System.out.println("Ошибка отправки файла с сервера, соединение потеряно");
                    connectionHandlers.remove(connectionHandler);
                }
            }
        }
    }
}