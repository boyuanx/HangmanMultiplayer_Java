package server;

import util.configReader;

import java.util.Map;

public class ServerMain {

    public static void main(String[] args) {
        Map<String, String> map;
        try {
            map = configReader.promptUserForConfig(true);
            SecretWordUtil.initWordList(map.get("SecretWordFile"));
            new HangmanServer(Integer.parseInt(map.get("ServerPort")));
        } catch (NullPointerException e) {
            System.exit(69);
        }
    }

}
