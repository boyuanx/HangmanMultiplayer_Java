package client;

import message.Message;
import util.GlobalScanner;
import util.jdbc_server_client_Util;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;

public class HangmanClient extends Thread {

    private String username;
    private ObjectInputStream ois;
    private ObjectOutputStream oos;
    private Socket s;

    public void run() {
        try {
            while (true) {
                Message m = (Message) ois.readObject();
                System.out.println(m.getUsername() + ": " + m.getMessage());
            }
        } catch (IOException | ClassNotFoundException e) {
            e.printStackTrace();
        }
    }

    HangmanClient(String hostname, int port) {
        try {
            System.out.println("Trying to connect to server...");
            s = new Socket(hostname, port);
            System.out.println("Connected!");

            username = jdbc_server_client_Util.userLogin();
            runListener();
        } catch (IOException e) {
            System.out.println("Unable to connect to server " + hostname + " on port " + port + ".");
            e.printStackTrace();
        }
        try {
            if (s != null) {
                s.close();
            }
            if (oos != null) {
                oos.close();
            }
            if (ois != null) {
                ois.close();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void runListener() throws IOException {
        oos = new ObjectOutputStream(s.getOutputStream());
        ois = new ObjectInputStream(s.getInputStream());
        this.start();
        while (true) {
            String line = GlobalScanner.getScanner().nextLine();
            Message m = new Message(username, line);
            oos.writeObject(m);
            oos.flush();
        }
    }

    public void makeNewGame(String gameName, int gameSize) {

    }
}
