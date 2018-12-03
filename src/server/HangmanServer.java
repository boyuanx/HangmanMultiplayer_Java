package server;

import gameRoom.GameRoom;
import message.Message;
import message.MessageType;
import util.TimestampUtil;
import util.jdbc_server_client_Util;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

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

	void tryStartAllThreadsInRoom(GameRoom g, HangmanServerThread hst) {
		if (g.isFull()) {
			for (HangmanServerThread thread : g.getClientThreads().values()) {
				thread.start();
			}
		} else {
			TimestampUtil.printMessage(hst.username + " - " + g.getGameName() + " needs " + g.getGameSize() + " to start game.");
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

	void broadcastExcludeUser(Message m, String username, HangmanServerThread hst) {
		GameRoom g = getGameRoomForUser(username);
		ConcurrentHashMap<String, HangmanServerThread> map = g.getClientThreads();
		Set<String> usernames = map.keySet();
		//usernames.remove(username);
		for (String user : usernames) {
			if (user == username) {
				continue;
			}
			map.get(user).sendMessage(m);
		}
	}

	void broadcastOnlyUser(Message m, String username, HangmanServerThread hst, boolean shouldIncrementGameRoomIndex) {
		GameRoom g = getGameRoomForUser(username);
		g.getClientThreads().get(username).sendMessage(m);
		if (shouldIncrementGameRoomIndex) {
			GlobalServerThreads.gameRooms.remove(g);
			g.currentBroadcastIndex++;
			if (g.currentBroadcastIndex == g.getGameSize()) {
				g.currentBroadcastIndex = 0;
			}
			GlobalServerThreads.gameRooms.add(g);
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

	GameRoom getGameRoomForUser(String username) {
		for (GameRoom g : GlobalServerThreads.gameRooms) {
			if (g.containsClient(username)) {
				return g;
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

		String usernames = "";
		String wins = "";
		String losses = "";
		usernames += winner.username + ",";
		Map<String, String> map = jdbc_server_client_Util.getStats(winner.username);
		wins += map.get("wins") + ",";
		losses += map.get("losses") + ",";
		for (String s: g.getClientThreads().keySet()) {
			if (s.equals(winner.username)){
				continue;
			}
			usernames += s + ",";
			map = jdbc_server_client_Util.getStats(s);
			wins += map.get("wins") + ",";
			losses += map.get("losses") + ",";
		}
		usernames = usernames.substring(0, usernames.length()-1);
		wins = wins.substring(0, wins.length()-1);
		losses = losses.substring(0, losses.length()-1);
		Message stats = new Message();
		stats.setMessageType(MessageType.WINSLOSSES);
		stats.putData("usernames", usernames);
		stats.putData("wins", wins);
		stats.putData("losses", losses);

		Message kill = new Message();
		kill.setMessageType(MessageType.KILL);
		kill.putData("message", "Thank you for playing Hangman!");

		for (HangmanServerThread thread : g.getClientThreads().values()) {
			if (thread.equals(winner)) {
				thread.sendMessage(win);
			} else {
				thread.sendMessage(lose);
			}
			thread.sendMessage(stats);
			thread.sendMessage(kill);
		}
	}

	void broadcastDisconnectToUser(HangmanServerThread hst) {
		GameRoom g = GlobalServerThreads.findGameRoom(hst);
		Message lose = new Message();
		lose.setMessageType(MessageType.SERVEROTHERRESPONSE);
		lose.putData("message", "That is incorrect! You lose!\nThe word was " + g.secretWord + ".");
		hst.sendMessage(lose);

		String usernames = "";
		String wins = "";
		String losses = "";
		Map<String, String> map;
		for (String s: g.getClientThreads().keySet()) {
			usernames += s + ",";
			map = jdbc_server_client_Util.getStats(s);
			wins += map.get("wins") + ",";
			losses += map.get("losses") + ",";
		}
		usernames = usernames.substring(0, usernames.length()-1);
		wins = wins.substring(0, wins.length()-1);
		losses = losses.substring(0, losses.length()-1);
		Message stats = new Message();
		stats.setMessageType(MessageType.WINSLOSSES);
		stats.putData("usernames", usernames);
		stats.putData("wins", wins);
		stats.putData("losses", losses);
		hst.sendMessage(stats);

		Message kill = new Message();
		kill.setMessageType(MessageType.KILL);
		kill.putData("message", "Thank you for playing Hangman!");
		hst.sendMessage(kill);
	}

	void broadcastOtherUserDisconnect(HangmanServerThread hst) {
		GameRoom g = GlobalServerThreads.findGameRoom(hst);
		Message lose = new Message();
		lose.setMessageType(MessageType.SERVEROTHERRESPONSE);
		lose.putData("message", hst.username + " fucked it up! Be gone!");
		hst.sendMessage(lose);
	}

}
