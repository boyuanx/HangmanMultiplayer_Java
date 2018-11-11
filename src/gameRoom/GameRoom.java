package gameRoom;

import java.util.HashMap;
import java.util.Vector;

public class GameRoom {

    private String gameName;
    private int gameSize;
    private Vector<String> clientList;

    public GameRoom(String name, int size, String creator) {
        gameName = name;
        gameSize = size;
        clientList = new Vector<>();
        clientList.add(creator);
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

    public void setGameSize(int gameSize) {
        this.gameSize = gameSize;
    }

    public boolean containsClient(String username) {
        return clientList.contains(username);
    }

    public boolean addClient(String username) {
        if (clientList.contains(username) || clientList.size() >= gameSize) {
            return false;
        }
        clientList.add(username);
        return true;
    }

    public boolean removeClient(String username) {
        if (!clientList.contains(username)) {
            return false;
        }
        clientList.remove(username);
        return true;
    }
}
