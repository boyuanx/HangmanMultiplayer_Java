package server;

import gameRoom.GameRoom;
import message.Message;
import message.MessageType;
import util.AlreadyLoggedInException;

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

    public static boolean doesGameRoomExist(String name) {
        boolean e = false;
        for (GameRoom g : gameRooms) {
            if (g.getGameName().equals(name)) {
                e = true;
            }
        }
        return e;
    }

    private static GameRoom findGameRoom(String gameName) {
        for (GameRoom g : gameRooms) {
            if (g.getGameName().equals(gameName)) {
                return g;
            }
        }
        return null;
    }

}
