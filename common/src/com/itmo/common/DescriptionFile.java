package com.itmo.common;

import java.io.Serializable;
import java.time.LocalDateTime;

public class DescriptionFile implements Serializable { // аналог класса Message
    // Нужен для рассылки информации о файле загруженном на сервер
    private String sender;
    private String fileName;
    private String description;
    private String text;
    private TxtFile file;

    public DescriptionFile(TxtFile file) {
        if (file == null) {
            System.out.println("Файл не может быть null");
            return;
        }
        this.file = file;
    }

    public void fillingOutTheFile() {
        sender = file.getSender();
        fileName = file.getFileName();
        description = file.getDescription();
        text = file.getText();
    }

    public String getSender() {
        return sender;
    }

    public String getFileName() {
        return fileName;
    }

    public String getDescription() {
        return description;
    }

    public String getText() {
        return text;
    }

    @Override
    public String toString() {
        return "DescriptionFile{" +
                "sender='" + sender + '\'' +
                ", fileName='" + fileName + '\'' +
                ", description='" + description + '\'' +
                ", text='" + text + '\'' +
                ", file=" + file +
                '}';
    }
}
