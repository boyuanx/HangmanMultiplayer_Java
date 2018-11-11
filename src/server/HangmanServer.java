package server;

import message.Message;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Vector;

class HangmanServer {

	private Vector<HangmanServerThread> serverThreads;
	
	HangmanServer(int port) {
		ServerSocket ss = null;
		try {
			ss = new ServerSocket(port);
			serverThreads = new Vector<>();
			while (true) {
				Socket s = ss.accept();
				HangmanServerThread hst = new HangmanServerThread(s, this);
				serverThreads.add(hst);
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
		for (HangmanServerThread thread : serverThreads) {
			if (m != null) {
				System.out.println(m.getMessage());
				thread.sendMessage(m);
			}
		}
	}

}
