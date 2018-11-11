package server;

import gameRoom.GameRoom;
import message.Message;
import util.GlobalScanner;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.HashMap;
import java.util.Vector;

class HangmanServer {

	//private Vector<HangmanServerThread> serverThreads;
	//private Vector<GameRoom> gameRooms;
	
	HangmanServer(int port) {
		ServerSocket ss = null;
		try {
			ss = new ServerSocket(port);
			GlobalServerThreads.serverThreads = new HashMap<>();
			GlobalServerThreads.gameRooms = new Vector<>();
			while (true) {
				Socket s = ss.accept();
				HangmanServerThread hst = new HangmanServerThread(s, this);
				//GlobalServerThreads.serverThreads.add(hst);
			}
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			try {
				if (ss != null) {
					ss.close();
				}
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}

	void broadcast(Message m, HangmanServerThread hst) {

		for (GameRoom g : GlobalServerThreads.gameRooms) {
			if (g.containsClient(hst)) {
				g.getClientThreads().forEach((k, v) -> v.sendMessage(m));
			}
		}

		/*
		for (HangmanServerThread thread : GlobalServerThreads.serverThreads) {
			if (m != null) {
				System.out.println(m.getMessage());
				thread.sendMessage(m);
			}
		}
		*/
	}

}
