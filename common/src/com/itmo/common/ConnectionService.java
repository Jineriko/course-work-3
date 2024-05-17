package com.itmo.common;

import java.io.*;
import java.net.Socket;
import java.time.LocalDateTime;

public class ConnectionService implements AutoCloseable {
    private ObjectInputStream inputStream;
    private ObjectOutputStream outputStream;
    private Socket socket;
    private boolean flagMessage = false; // флаг, использующийся для рассылки сообщений сервером,
    // чтобы самому себе сообщение не приходило
    private boolean flagDescriptionFile = false; // флаг, использующийся для рассылки описания файла сервером,

    // чтобы самому себе сообщение не приходило
    public ConnectionService(Socket socket) throws IOException {
        this.socket = socket;
        outputStream = new ObjectOutputStream(socket.getOutputStream());
        inputStream = new ObjectInputStream(socket.getInputStream());
    }

    public boolean isFlagMessage() {
        return flagMessage;
    }

    public void setFlagMessage(boolean flagMessage) {
        this.flagMessage = flagMessage;
    }

    public boolean isFlagDescriptionFile() {
        return flagDescriptionFile;
    }

    public void setFlagDescriptionFile(boolean flagDescriptionFile) {
        this.flagDescriptionFile = flagDescriptionFile;
    }


    public void sendMessage(Message message) { // метод отправки message
        if (message == null) {
            System.out.println("Сообщение не может быть null.\n " +
                    "Отправка сообщения невозможна");
            return;
        }
        message.setSentAt(LocalDateTime.now());
        try {
            outputStream.writeObject(message);
            outputStream.flush();
        } catch (IOException e) {
            System.out.println("Ошибка отправки сообщения");
        }
    }

    public void sendDescriptionFile(TxtFile file) { // отправка описания файла для клиента
        if (file == null) {
            System.out.println("Файл не может быть null.\n " +
                    "Рассылка описания файла невозможна");
            return;
        }
        DescriptionFile descriptionFile = new DescriptionFile(file);
        descriptionFile.fillingOutTheFile();
        try {
            outputStream.writeObject(descriptionFile);
            outputStream.flush();
        } catch (IOException e) {
            System.out.println("Ошибка отправки описания файла");
        }
    }

    public void sendFile(TxtFile txtFile) { // метод отправки файла по сокет соединению
        if (txtFile == null) {
            System.out.println("Файл не может быть null.\n " +
                    "Отправка файла невозможна");
            return;
        }

        try {
            outputStream.writeObject(txtFile);
            outputStream.flush();
        } catch (IOException e) {
            System.out.println("Ошибка отправки файла");
        }
    }

    public void sendInt(int request) { // отправка запроса, для получения списка файлов
        try {
            outputStream.writeObject(request);
            outputStream.flush();
        } catch (IOException e) {
            System.out.println("Ошибка отправки запроса");
        }
    }
    public void sendString(String string) { // отправка запроса, для получения списка файлов
        try {
            outputStream.writeObject(string);
            outputStream.flush();
        } catch (IOException e) {
            System.out.println("Ошибка отправки запроса");
        }
    }

    public Object read() throws IOException { // метод получения объекта (строки, message или файла) по сокет соединению
        Object object = null;
        try {
            object = inputStream.readObject();
            return object;

        } catch (ClassNotFoundException e) {
            System.out.println("Не удалось получить объект");
            throw new RuntimeException(e);
        }
    }

    public void writeTxtFile(String fileName, TxtFile file) { // метод записи файла для хранения на сервере или клиенте
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(fileName))) {
            writer.write(file.getSender() + "::" +
                    file.getFileName() + "::" +
                    file.getDescription() + "::" +
                    file.getText());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void close() throws Exception {
        inputStream.close();
        outputStream.close();
        socket.close();
    }
}
