package server;

import gameRoom.GameRoom;
import message.Message;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.HashMap;
import java.util.Map;
import java.util.Vector;

class HangmanServer {

	HangmanServer(int port) {
		ServerSocket ss = null;
		try {
			ss = new ServerSocket(port);
			GlobalServerThreads.serverThreads = new HashMap<>();
			GlobalServerThreads.gameRooms = new Vector<>();
			while (true) {
				Socket s = ss.accept();
				HangmanServerThread hst = new HangmanServerThread(s, this);
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
	}

	void broadcastExcludeSelf(Message m, HangmanServerThread hst) {
		for (GameRoom g : GlobalServerThreads.gameRooms) {
			if (g.containsClient(hst)) {
				Map<String, HangmanServerThread> map = g.getClientThreads();
				for (HangmanServerThread thread : map.values()) {
					if (!thread.equals(hst)) {
						thread.sendMessage(m);
					}
				}
			}
		}
	}

}
