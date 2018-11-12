package server;

import gameRoom.GameRoom;
import message.Message;
import message.MessageType;
import util.WrongPasswordException;
import util.jdbc_server_client_Util;

import java.io.EOFException;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.lang.invoke.WrongMethodTypeException;
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
            clientAuthentication();
            waitForClientToJoinRoom();
            this.start();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void clientAuthentication() {
        Message response = new Message();
        try {
            Object o = ois.readObject();
            Message m = (Message)o;
            if (m.getMessageType() == MessageType.AUTHENTICATION) {
                String username = (String)m.getData("username");
                String password = (String)m.getData("password");
                response.setMessageType(MessageType.AUTHENTICATION);
                if (jdbc_server_client_Util.userAuth(username, password)) {
                    System.out.println("User authenticated");
                    response.putData("response", 1);
                } else {
                    response.putData("response", 0);
                }
            }
        } catch (WrongPasswordException e) {
            response.putData("response", -1);
        } catch (IOException | ClassNotFoundException e) {
            e.printStackTrace();
        }
        sendMessage(response);
    }

    private void waitForClientToJoinRoom() {
        try {
            while (true) {
                Object o = ois.readObject();
                Message m = (Message)o;
                if (m.getMessageType() == MessageType.NEWGAMECONFIG) {
                    GameRoom g = new GameRoom((String) m.getData("gameName"), (int) m.getData("gameSize"), m.getUsername(), this);
                    GlobalServerThreads.gameRooms.add(g);
                    break;
                } else if (m.getMessageType() == MessageType.JOINGAMEINFO) {
                    GameRoom g = null;
                    for (GameRoom gg : GlobalServerThreads.gameRooms) {
                        if (gg.getGameName().equalsIgnoreCase((String) m.getData("gameName"))) {
                            g = gg;
                            GlobalServerThreads.gameRooms.remove(gg);
                            break;
                        }
                    }
                    g.addClient(m.getUsername(), this);
                    GlobalServerThreads.gameRooms.add(g);
                    break;
                } else {
                    System.err.println("Expected handshake, received " + m.getMessageType() + " instead.");
                }
            }
        } catch (IOException e) {
            System.err.println("Handshake with incoming client has failed: " + e.getMessage());
        } catch (ClassNotFoundException e) {
            System.err.println("Incoming client handshake corrupted: " + e.getMessage());
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
