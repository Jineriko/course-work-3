package com.itmo.common;

import java.io.File;
import java.io.Serializable;

public class TxtFile extends File implements Serializable {
    private String sender;
    private String fileName;
    private String description;
    private String text;

    public TxtFile(String sender, String fileName) {
        super(fileName);
        if (sender == null) {
            System.out.println("Поле отправитель не может быть null");
            return;
        }
        this.sender = sender;
        this.fileName = fileName;
    }

    public String getSender() {
        return sender;
    }

    public String getFileName() {
        return fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }

    @Override
    public String toString() {
        return sender + "::" + fileName + "::" + description+  "::" + text;
    }
}
