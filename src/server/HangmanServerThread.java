package server;

import gameRoom.GameRoom;
import message.Message;
import message.MessageType;
import util.TimestampUtil;
import util.WrongPasswordException;
import util.jdbc_server_client_Util;

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
            clientAuthentication();
            waitForClientToJoinRoom();
            this.start();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void clientAuthentication() {
        Message response = new Message();
        String tempU = null;
        try {
            Object o = ois.readObject();
            Message m = (Message)o;
            MessageType type = m.getMessageType();
            if (type == MessageType.AUTHENTICATION) {
                String username = (String)m.getData("username");
                tempU = username;
                String password = (String)m.getData("password");

                TimestampUtil.printMessage(username + " - trying to log in with password " + password + ".");

                response.setMessageType(MessageType.AUTHENTICATION);
                if (jdbc_server_client_Util.userAuth(username, password)) {
                    TimestampUtil.printMessage(username + " - successfully logged in.");
                    response.putData("response", 1);
                    sendMessage(response);
                } else {
                    TimestampUtil.printMessage(username + " - does not have an account. Not successfully logged in.");
                    response.putData("response", -1);
                    sendMessage(response);
                    clientAuthentication();
                }
            } else if (type == MessageType.MAKEACCOUNT) {
                String username = (String)m.getData("username");
                tempU = username;
                String password = (String)m.getData("password");
                response.setMessageType(MessageType.MAKEACCOUNT);
                if (jdbc_server_client_Util.serverMakeAccount(username, password)) {
                    TimestampUtil.printMessage(username + " - created an account with password " + password);
                    TimestampUtil.printMessage(username + " - has record " + jdbc_server_client_Util.getWins() + " wins and " + jdbc_server_client_Util.getLosses() + " losses.");
                    response.putData("response", 1);
                    response.putData("username", username);
                    response.putData("password", password);
                    response.putData("wins", jdbc_server_client_Util.getWins());
                    response.putData("losses", jdbc_server_client_Util.getLosses());
                    sendMessage(response);
                } else {
                    TimestampUtil.printMessage(username + " - failed to create a new account.");
                    response.putData("response", 0);
                    sendMessage(response);
                }
            }
        } catch (WrongPasswordException e) {
            TimestampUtil.printMessage(tempU + " - has an account but not successfully logged in.");
            response.putData("response", 0);
            sendMessage(response);
            clientAuthentication();
        } catch (IOException | ClassNotFoundException e) {
            e.printStackTrace();
        }
    }

    private void waitForClientToJoinRoom() {
        try {
            while (true) {
                Object o = ois.readObject();
                Message m = (Message)o;
                if (m.getMessageType() == MessageType.NEWGAMECONFIG) {
                    TimestampUtil.printMessage(m.getUsername() + " wants to start a game called " + m.getData("gameName") + ".");
                    GameRoom g = new GameRoom((String) m.getData("gameName"), (int) m.getData("gameSize"), m.getUsername(), this);
                    GlobalServerThreads.gameRooms.add(g);

                    Message r = new Message();
                    r.setMessageType(MessageType.NEWGAMECONFIG);
                    r.putData("response", 1);
                    r.putData("message", g.getRemainingCapacityMessage());
                    sendMessage(r);

                    TimestampUtil.printMessage(m.getUsername() + " successfully started game " + m.getData("gameName") + ".");
                    break;
                } else if (m.getMessageType() == MessageType.JOINGAMEINFO) {
                    TimestampUtil.printMessage(m.getUsername() + " wants to join a game called " + m.getData("gameName") + ".");
                    GameRoom g = null;
                    for (GameRoom gg : GlobalServerThreads.gameRooms) {
                        if (gg.getGameName().equalsIgnoreCase((String) m.getData("gameName"))) {
                            g = gg;
                            GlobalServerThreads.gameRooms.remove(gg);
                            break;
                        }
                    }

                    if (g == null) {
                        Message r = new Message();
                        r.setMessageType(MessageType.JOINGAMEINFO);
                        r.putData("response", 0);
                        r.putData("message", "There is no game with name " + m.getData("gameName") + ".");
                        sendMessage(r);
                        break;
                    }

                    g.addClient(m.getUsername(), this);
                    GlobalServerThreads.gameRooms.add(g);

                    Message r = new Message();
                    r.setMessageType(MessageType.JOINGAMEINFO);
                    r.putData("response", 1);
                    r.putData("message", g.getRemainingCapacityMessage());
                    sendMessage(r);

                    TimestampUtil.printMessage(m.getUsername() + " successfully joined game " + m.getData("gameName") + ".");
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
