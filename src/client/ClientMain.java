package client;

import util.configReader;

import java.util.Map;
import java.util.Scanner;

public class ClientMain {

    public static void main(String[] args) {
        Map<String, String> map;
        try {
            map = configReader.promptUserForConfig();
            HangmanClient client = new HangmanClient(map.get("ServerHostname"), Integer.parseInt(map.get("ServerPort")));
            // The above code will end at outputting wins and losses.


        } catch (NullPointerException e) {
            e.printStackTrace();
            System.exit(69);
        }
    }

}