package com.itmo.client;

import com.itmo.common.ConnectionService;
import com.itmo.common.DescriptionFile;
import com.itmo.common.Message;
import com.itmo.common.TxtFile;

import java.io.*;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.Scanner;

public class Client {
    private final InetSocketAddress address;
    private String username;
    private final Scanner scanner;
    private ConnectionService connectionHandler;

    public Client(InetSocketAddress address) {
        this.address = address;
        scanner = new Scanner(System.in);
    }

    private void createConnection() throws IOException {
        connectionHandler = new ConnectionService(
                new Socket(address.getHostName(), address.getPort()));

    }

    private class Writer extends Thread {
        public void run() {
            while (true) {
                System.out.println("Что делаем : \n" +
                        "1. Отправляем на сервер сообщение \n" +
                        "2. Отправляем на сервер файл с расширением '.txt' \n" +
                        "3. Запрашиваем файл с сервера \n" +
                        "4. Выход из программы \n");
                String text = scanner.nextLine();
                int chooseUser = 0;
                try {
                    chooseUser = Integer.parseInt(text);
                } catch (Exception e) {
                    System.out.println("Введено некорректное значение");
                    continue;
                }

                switch (chooseUser) { // отправка запроса на сервер, в зависимости от выбора юзера
                    case 1 -> {
                        System.out.println("Введите текст сообщения");
                        String textMessage = scanner.nextLine();
                        if (textMessage.length() < 1 || textMessage == null) {
                            System.out.println("Сообщение не может быть пустым");
                            continue;
                        }
                        System.out.println();
                        Message message = new Message(username);
                        message.setText(textMessage);
                        synchronized (connectionHandler) {
                            try {
                                connectionHandler.sendMessage(message);
                                System.out.println("Сообщение успешно отправлено\n");
                            } catch (Exception e) {
                                System.out.println("Сообщение не было отправлено");
                            }
                        }
                    }
                    case 2 -> {
                        System.out.println("Введите имя файла");
                        String fileName = scanner.nextLine();
                        if (fileName == null) { // проверка на null
                            System.out.println("Имя файла не может быть пустым");
                            continue;
                        }
                        if (!fileName.endsWith(".txt")) { // проверка на тип файла
                            System.out.println("Файл должен иметь расширение '.txt'");
                            continue;
                        }

                        // считывание файла, который ввел юзер
                        String[] elementInString = new String[4];
                        try (BufferedReader reader = new BufferedReader(new FileReader(fileName))) {
                            String line;
                            while ((line = reader.readLine()) != null) {
                                elementInString = line.split("::");
                            }
                        } catch (FileNotFoundException e) {
                            System.out.println("Не найден указанный файл\n");
                            continue;
                        } catch (IOException e) {
                            System.out.println("Ошибка чтения файла\n");
                            continue;
                        }


                        // создание этого файла
                        TxtFile fileUser = new TxtFile(username, elementInString[1]);
                        fileUser.setDescription(elementInString[2]);
                        fileUser.setText(elementInString[3]);

                        if (fileUser.getFileName() == null) {
                            System.out.println("Файл не найден");
                            continue;
                        }
                        if (fileUser.getAbsolutePath().length() > 100) {
                            System.out.println("Название файла превышает допустимый размер в 100 символов.\n " +
                                    "Отправка файла невозможна");
                            return;
                        }
                        long fileSize = fileUser.length() / 1_048_576; // размер файла в Мб
                        if (fileSize > 10) { // проверка на ограничение файла по Мб
                            System.out.println("Размер файла больше 10 Мб.\n " +
                                    "Отправка файла невозможна");
                            return;
                        }
                        synchronized (connectionHandler) {
                            try {
                                connectionHandler.sendFile(fileUser); // отправка файла на сервер
                                System.out.println("Файл успешно отправлен\n");
                            } catch (Exception e) {
                                System.out.println("Ошибка отправки файла");
                            }
                        }
                    }
                    case 3 -> {
                        Integer request = chooseUser;
                        synchronized (connectionHandler) {
                            try {
                                connectionHandler.sendInt(request);
                            } catch (Exception e) {
                                System.out.println("Ошибка отправки запроса");
                            }
                        }
                        getFileFromServer();

                    }
                    case 4 -> {
                        System.out.println("Отключение от сервера");
                        System.exit(0);
                    }
                    default -> System.out.println("Некорректный ввод");
                }
            }
        }
        private void getFileFromServer() {
            String requestUser = scanner.nextLine(); // юзер выбирает нужный файл, по имени файла
            try {
                connectionHandler.sendString(requestUser);
            } catch (Exception e) {
                System.out.println("Ошибка отправки запроса");
            }
            try {
                Thread.sleep(200); // для ожидания получения файла с сервера
                // перед выводом меню
            } catch (InterruptedException e) {

            }
        }
    }

    private class Reader extends Thread {
        public void run() {
            while (true) {
                Object inputObject = null;

                try {
                    inputObject = connectionHandler.read();
                    if (inputObject instanceof Message) {
                        System.out.println(((Message) inputObject).getText());
                    } else if (inputObject instanceof DescriptionFile) {
                        System.out.println("\n" + "Cервер получил файл: " + ((DescriptionFile) inputObject).getFileName() +
                                " \n" + "описание файла: " + ((DescriptionFile) inputObject).getDescription() + " \n" +
                                "от пользователя: " + ((DescriptionFile) inputObject).getSender() + "\n");
                    } else if (inputObject instanceof String) {
                        if (inputObject.equals("На сервере нет доступных фалов")) {
                            System.out.println("На сервере нет доступных фалов. \nНажмите любую кнопку для продолжения");
                            return;
                        } else {
                            System.out.println("Список доступных файлов: ");
                            System.out.println(inputObject + "\n");
                        }
                    } else if (inputObject instanceof TxtFile) {
                        System.out.println("Получен файл");
                        System.out.println(inputObject + "\n");

                    } else {
                        System.out.println("Не удалось прочитать");
                    }
                } catch (IOException e) {
                    System.out.println("Не удалось получить сообщение от сервера");
                    break;
                }
            }
        }

    }

    public void startClient() /*throws Exception*/ {
        System.out.println("Введите имя");
        username = scanner.nextLine();
        try {
            createConnection();
        } catch (IOException e) {
            System.out.println("Не удалось установить соединение с сервером");
            System.exit(0);
        }
        new Writer().start();
        new Reader().start();
    }
}
