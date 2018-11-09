package client;

import server.Message;
import util.GlobalScanner;
import util.configReader;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.util.Scanner;

public class HangmanClient extends Thread {

    private String username;
    private ObjectInputStream ois;
    private ObjectOutputStream oos;

    public HangmanClient(String hostname, int port) {
        Socket s = null;
        try {
            System.out.println("Trying to connect to server...");
            s = new Socket(hostname, port);
            System.out.println("Connected!");
            oos = new ObjectOutputStream(s.getOutputStream());
            ois = new ObjectInputStream(s.getInputStream());
            this.start();
            runListener();
        } catch (IOException e) {
            System.out.println("Unable to connect to server " + hostname + " on port " + port + ".");
            e.printStackTrace();
        }
        finally {
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
    }

    private void runListener() throws IOException {
        while (true) {
            String line = GlobalScanner.getScanner().nextLine();
            System.out.println("Read: " + line);
            Message m = new Message(username, line);
            oos.writeObject(m);
            oos.flush();
        }
    }

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
}
