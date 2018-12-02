package gameRoom;

import server.HangmanServerThread;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.stream.Stream;

public class GameRoom {

    private String gameName;
    private int gameSize;
    private LinkedHashMap<String, HangmanServerThread> clientThreads;
    public int currentBroadcastIndex = 0;
    public int guessesLeft = 7;
    public String secretWord;
    public String secretWordMutable;

    public GameRoom(String name, int size, String creator, HangmanServerThread thread) {
        gameName = name;
        gameSize = size;
        clientThreads = new LinkedHashMap<>();
        clientThreads.put(creator, thread);
    }

    public String getGameName() {
        return gameName;
    }

    public void setGameName(String gameName) {
        this.gameName = gameName;
    }

    public int getGameSize() {
        return gameSize;
    }

    public boolean isFull() {
        return gameSize < clientThreads.size();
    }

    public String getRemainingCapacityMessage() {
        int i = gameSize - clientThreads.size();
        if (i == 0) {
            return "All users have joined.";
        } else {
            return "Waiting for " + Integer.toString(i) + " other user(s) to join...";
        }
    }

    public void setGameSize(int gameSize) {
        this.gameSize = gameSize;
    }

    public boolean containsClient(String username) {
        return clientThreads.containsKey(username);
    }

    public boolean containsClient(HangmanServerThread thread) {
        return clientThreads.containsValue(thread);
    }

    public boolean addClient(String username, HangmanServerThread thread) {
        if (clientThreads.containsKey(username) || clientThreads.size() >= gameSize) {
            return false;
        }
        clientThreads.put(username, thread);
        return true;
    }

    public boolean removeClient(String username) {
        if (!clientThreads.containsKey(username)) {
            return false;
        }
        clientThreads.remove(username);
        return true;
    }

    public LinkedHashMap<String, HangmanServerThread> getClientThreads() {
        return clientThreads;
    }

    public void setSecretWord(String s) {
        if (secretWord == null) {
            secretWord = s;
            secretWordMutable = s;
        }
    }

    public boolean isLetterInWord(String s) {
        boolean result = secretWordMutable.contains(s);
        if (result) {
            secretWordMutable = secretWordMutable.replaceFirst(s, "");
        }
        return result;
    }

}
