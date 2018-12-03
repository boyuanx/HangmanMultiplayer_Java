package client;

import util.configReader;

import java.io.EOFException;
import java.util.Map;
import java.util.Scanner;

public class ClientMain {

    public static void main(String[] args) {
        Map<String, String> map;
        while (true) {
            try {
                map = configReader.promptUserForConfig(false);
                new HangmanClient(map.get("ServerHostname"), Integer.parseInt(map.get("ServerPort")));
            } catch (NullPointerException e) {
                e.printStackTrace();
            }
        }
    }

}