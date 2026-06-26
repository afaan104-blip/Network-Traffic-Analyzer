package org.example;

public class Main {
    public static void main(String[] args) {
        try {
            ApiServer.start();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
