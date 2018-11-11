package server;

import message.Message;

import java.io.EOFException;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;

public class HangmanServerThread extends Thread {

    private ObjectInputStream ois;
    private ObjectOutputStream oos;
    private HangmanServer hs;

    HangmanServerThread(Socket s, HangmanServer hs) {
        try {
            ois = new ObjectInputStream(s.getInputStream());
            oos = new ObjectOutputStream(s.getOutputStream());
            this.hs = hs;
            this.start();
            //establishHandShake();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    void establishHandShake() {
        while (true) {

        }
    }

    void sendMessage(Message m) {
        try {
            oos.writeObject(m);
            oos.flush();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void run() {
        try {
            while (true) {
                Message m = (Message)ois.readObject();
                if (m != null) {
                    hs.broadcast(m, this);
                } else {
                    System.out.println("NULL in HangmanServerThread:run()!");
                }
            }
        } catch (EOFException e) {
            System.err.println("Client disconnected.");
        } catch (IOException | ClassNotFoundException e) {
            e.printStackTrace();
        }
    }

}
