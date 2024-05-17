package com.itmo.server;

public class ServerApp {
    public static void main(String[] args) {
        new Server(2223).startServer();
    }
}
