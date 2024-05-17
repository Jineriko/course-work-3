package com.itmo.common;

import java.io.*;

public class Main {
    public static void main(String[] args) {
        TxtFile txtFile1 = new TxtFile("Nikita", "cats.txt");
        txtFile1.setDescription("About cats");
        txtFile1.setText("I like cats");

        TxtFile txtFile2 = new TxtFile("Katya", "dogs.txt");
        txtFile2.setDescription("About dogs");
        txtFile2.setText("I like dogs");

        TxtFile txtFile3 = new TxtFile("Elena", "snakes.txt");
        txtFile3.setDescription("About snakes");
        txtFile3.setText("I like snakes");

        writeTxtFile(txtFile1.getFileName(), txtFile1);
        writeTxtFile(txtFile2.getFileName(), txtFile2);
        writeTxtFile(txtFile3.getFileName(), txtFile3);

        System.out.println("Done!");
    }
    public static void writeTxtFile(String fileName, TxtFile file) { // метод записи файла для хранения на сервере или клиенте
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(fileName))) {
            writer.write(file.getSender() + "::" +
                    file.getFileName() + "::" +
                    file.getDescription() + "::" +
                    file.getText());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
