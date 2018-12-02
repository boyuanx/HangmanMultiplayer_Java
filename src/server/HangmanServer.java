package server;

import gameRoom.GameRoom;
import jdk.nashorn.internal.objects.Global;
import message.Message;
import message.MessageType;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.*;

class HangmanServer {

	HangmanServer(int port) {
		ServerSocket ss = null;
		try {
			ss = new ServerSocket(port);
			GlobalServerThreads.serverThreads = new HashMap<>();
			GlobalServerThreads.gameRooms = new Vector<>();
			while (true) {
				Socket s = ss.accept();
				new HangmanServerThread(s, this);
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

	void broadcastGameRoom(Message m, HangmanServerThread hst) {
		for (GameRoom g : GlobalServerThreads.gameRooms) {
			if (g.containsClient(hst)) {
				g.getClientThreads().forEach((k, v) -> v.sendMessage(m));
				break;
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
				break;
			}
		}
	}

	void broadcastToCurrentUserAndIncrementIndex(Message m, HangmanServerThread hst) {
		hst.sendMessage(m);
		for (GameRoom g : GlobalServerThreads.gameRooms) {
			if (g.containsClient(hst)) {
				GlobalServerThreads.gameRooms.remove(g);
				g.currentBroadcastIndex++;
				if (g.currentBroadcastIndex == g.getGameSize()) {
					g.currentBroadcastIndex = 0;
				}
				GlobalServerThreads.gameRooms.add(g);
				break;
			}
		}
	}

	String getCurrentUserInRoom(HangmanServerThread hst) {
		for (GameRoom g : GlobalServerThreads.gameRooms) {
			if (g.containsClient(hst)) {
				int index = 0;
				int targetIndex = g.currentBroadcastIndex;
				Set<String> usernames = g.getClientThreads().keySet();
				for (String s : usernames) {
					if (index != targetIndex) {
						index ++;
					} else {
						return s;
					}
				}
			}
		}
		return null;
	}

	void broadcastWinLossKillToRoom(HangmanServerThread winner) {
		Message win = new Message();
		win.setMessageType(MessageType.SERVEROTHERRESPONSE);
		win.putData("message", "That is correct! You win!");

		Message lose = new Message();
		lose.setMessageType(MessageType.SERVEROTHERRESPONSE);
		lose.putData("message", winner.username + " guessed the word correctly. You lose!");

		GameRoom g = GlobalServerThreads.findGameRoom(winner);
		for (HangmanServerThread thread : g.getClientThreads().values()) {
			if (thread.equals(winner)) {
				thread.sendMessage(win);
			} else {
				thread.sendMessage(lose);
			}
		}
	}

}
