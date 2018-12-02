package server;

import gameRoom.GameRoom;
import message.Message;
import message.MessageType;
import util.AlreadyLoggedInException;
import util.TimestampUtil;

import java.util.ArrayList;
import java.util.Map;
import java.util.Vector;

public class GlobalServerThreads {

    static Map<String, HangmanServerThread> serverThreads;
    static Vector<GameRoom> gameRooms;

    static void addNewThread(String username, HangmanServerThread hst) throws AlreadyLoggedInException {
        if (serverThreads.containsKey(username)) {
            throw new AlreadyLoggedInException(username);
        } else {
            serverThreads.put(username, hst);
        }
    }

    static void removeThread(String username) {
        serverThreads.remove(username);
    }

    static GameRoom addGameRoom(Message m, HangmanServerThread thread) {
        MessageType type = m.getMessageType();
        if (type == MessageType.NEWGAMECONFIG) {
            if (!doesGameRoomExist((String)m.getData("gameName"))) {
                GameRoom g = new GameRoom((String) m.getData("gameName"), (int) m.getData("gameSize"), m.getUsername(), thread);
                GlobalServerThreads.gameRooms.add(g);
                return g;
            }
        } else if (type == MessageType.JOINGAMEINFO) {
            if (doesGameRoomExist((String)m.getData("gameName"))) {
                GameRoom g = findGameRoom((String)m.getData("gameName"));
                if (g.isFull()) {
                    return null;
                }
                GlobalServerThreads.gameRooms.remove(g);
                g.addClient(m.getUsername(), thread);
                GlobalServerThreads.gameRooms.add(g);
                return g;
            }
        }
        return null;
    }

    static boolean doesGameRoomExist(String name) {
        boolean e = false;
        for (GameRoom g : gameRooms) {
            if (g.getGameName().equals(name)) {
                if (g.getClientThreads().isEmpty()) {
                    gameRooms.remove(g);
                } else {
                    e = true;
                }
                break;
            }
        }
        return e;
    }

    static GameRoom findGameRoom(String gameName) {
        for (GameRoom g : gameRooms) {
            if (g.getGameName().equals(gameName)) {
                return g;
            }
        }
        return null;
    }

    static GameRoom findGameRoom(HangmanServerThread hst) {
        for (GameRoom g : gameRooms) {
            if (g.containsClient(hst)) {
                return g;
            }
        }
        return null;
    }

    static void setSecretWordForRoom(String s, HangmanServerThread hst) {
        GameRoom g = findGameRoom(hst);
        if (g.secretWord != null) {
            TimestampUtil.printMessage(hst.username + " - Secret word has already been set!");
            return;
        }
        gameRooms.remove(g);
        g.setSecretWord(s);
        gameRooms.add(g);
    }

    static boolean checkIfLetterInWordForRoom(String s, HangmanServerThread hst) {
        GameRoom g = findGameRoom(hst);
        gameRooms.remove(g);
        boolean result = g.isLetterInWord(s);
        gameRooms.add(g);
        return result;
    }

    static boolean checkWordGuessForRoom(String s, HangmanServerThread hst) {
        GameRoom g = findGameRoom(hst);
        return g.secretWord.equalsIgnoreCase(s);
    }

    static void removeClientFromRooms(String username) {
        gameRooms.forEach((v) -> v.removeClient(username));
    }

}
