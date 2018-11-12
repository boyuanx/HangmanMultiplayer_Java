package client;

import message.Message;
import message.MessageType;
import util.GlobalScanner;
import util.WrongPasswordException;
import util.jdbc_server_client_Util;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;

public class HangmanClient extends Thread {

    private String username;
    //private ObjectInputStream ois;
    //private ObjectOutputStream oos;
    //private Socket s;

    public void run() {
        try {
            while (true) {
                Message m = (Message) GlobalSocket.ois.readObject();
                MessageType type = m.getMessageType();
                if (type == MessageType.AUTHENTICATION) {
                    int response = (int)m.getData("response");
                    if (response == 1) {
                        jdbc_server_client_Util.loginSuccessMessage();
                        username = jdbc_server_client_Util.getUsername();
                    } else if (response == 0) {
                        throw new WrongPasswordException();
                    } else if (response == -1) {
                        if (jdbc_server_client_Util.makeAccountFromCredentials()) {
                            username = jdbc_server_client_Util.getUsername();
                        }
                    }
                }


                System.out.println("DEBUG: " + m.getUsername() + ": " + m.getMessage());
            }
        } catch (IOException | ClassNotFoundException e) {
            e.printStackTrace();
        } catch (WrongPasswordException e) {
            e.getMessage();
            jdbc_server_client_Util.userLogin();
        }
    }

    HangmanClient(String hostname, int port) {
        try {
            System.out.print("Trying to connect to server...");
            GlobalSocket.s = new Socket(hostname, port);
            System.out.println("Connected!");
            initObjectStreams();
            this.start();
            jdbc_server_client_Util.userLogin();
            runListener();
        } catch (IOException e) {
            System.out.println("Unable to connect to server " + hostname + " on port " + port + ".");
            e.printStackTrace();
        }
        try {
            if (GlobalSocket.s != null) {
                GlobalSocket.s.close();
            }
            if (GlobalSocket.oos != null) {
                GlobalSocket.oos.close();
            }
            if (GlobalSocket.ois != null) {
                GlobalSocket.ois.close();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void initObjectStreams() throws IOException {
        GlobalSocket.oos = new ObjectOutputStream(GlobalSocket.s.getOutputStream());
        GlobalSocket.ois = new ObjectInputStream(GlobalSocket.s.getInputStream());
    }


    private void runListener() throws IOException {
        while (true) {
            String line = GlobalScanner.getScanner().nextLine();
            Message m = new Message(username);
            m.setMessage(line);
            GlobalSocket.oos.writeObject(m);
            GlobalSocket.oos.flush();
        }
    }


}
