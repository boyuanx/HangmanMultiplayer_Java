package gameRoom;

import server.HangmanServerThread;

import java.util.HashMap;
import java.util.Map;

public class GameRoom {

    private String gameName;
    private int gameSize;
    private Map<String, HangmanServerThread> clientThreads;

    public GameRoom(String name, int size, String creator, HangmanServerThread thread) {
        gameName = name;
        gameSize = size;
        clientThreads = new HashMap<>();
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

    public Map<String, HangmanServerThread> getClientThreads() {
        return clientThreads;
    }
}
