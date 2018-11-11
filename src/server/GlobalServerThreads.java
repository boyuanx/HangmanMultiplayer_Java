package server;

import gameRoom.GameRoom;

import java.util.Map;
import java.util.Vector;

public class GlobalServerThreads {

    static Map<String, HangmanServerThread> serverThreads;
    static Vector<GameRoom> gameRooms;

}
