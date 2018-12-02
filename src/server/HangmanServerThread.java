package server;

import gameRoom.GameRoom;
import message.Message;
import message.MessageType;
import util.AlreadyLoggedInException;
import util.TimestampUtil;
import util.WrongPasswordException;
import util.jdbc_server_client_Util;

import java.io.EOFException;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.security.Timestamp;
import java.util.Map;

public class HangmanServerThread extends Thread {

    private ObjectInputStream ois;
    private ObjectOutputStream oos;
    private HangmanServer hs;
    private String username;

    HangmanServerThread(Socket s, HangmanServer hs) {
        try {
            ois = new ObjectInputStream(s.getInputStream());
            oos = new ObjectOutputStream(s.getOutputStream());
            this.hs = hs;
            this.start();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void clientAuthentication() throws AlreadyLoggedInException {
        Message response = new Message();
        String tempU = null;
        try {
            Object o = ois.readObject();
            Message m = (Message)o;
            MessageType type = m.getMessageType();
            String username = (String)m.getData("username");
            GlobalServerThreads.addNewThread(username, this);

            if (type == MessageType.AUTHENTICATION) {
                tempU = username;
                String password = (String)m.getData("password");

                TimestampUtil.printMessage(username + " - trying to log in with password " + password + ".");

                response.setMessageType(MessageType.AUTHENTICATION);
                if (jdbc_server_client_Util.userAuth(username, password)) {
                    TimestampUtil.printMessage(username + " - successfully logged in.");
                    TimestampUtil.printMessage(username + " - has record " + jdbc_server_client_Util.getWins() + " wins and " + jdbc_server_client_Util.getLosses() + " losses.");
                    response.putData("response", 1);
                    sendMessage(response);
                } else {
                    TimestampUtil.printMessage(username + " - does not have an account. Not successfully logged in.");
                    response.putData("response", -1);
                    sendMessage(response);
                    clientAuthentication();
                }
            } else if (type == MessageType.MAKEACCOUNT) {
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
                    this.username = username;
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
            TimestampUtil.printMessage("Client disconnected.");
        }
    }

    private void sendDeauthPacket() {
        try {
            Message m = new Message();
            m.setMessageType(MessageType.AUTHENTICATION);
            m.putData("response", -2);
            m.putData("message", "Oops wait, but you already have an active session! Bye bye get kicked!");
            oos.writeObject(m);
            oos.flush();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void waitForClientToJoinRoom() {
        try {
            while (true) {
                Object o = ois.readObject();
                Message m = (Message)o;
                if (m.getMessageType() == MessageType.NEWGAMECONFIG) {
                    TimestampUtil.printMessage(m.getUsername() + " - wants to start a game called " + m.getData("gameName") + ".");
                    GameRoom g = GlobalServerThreads.addGameRoom(m, this);

                    if (g == null) {
                        TimestampUtil.printMessage(m.getUsername() + " - " + m.getData("gameName") + " already exists, so unable to start " + m.getData("gameName") + ".");
                        Message r = new Message();
                        r.setMessageType(MessageType.NEWGAMECONFIG);
                        r.putData("response", 0);
                        r.putData("message", m.getData("gameName") + " already exists.");
                        sendMessage(r);
                        break;
                    }
                    Message r = new Message();
                    r.setMessageType(MessageType.NEWGAMECONFIG);
                    r.putData("response", 1);
                    r.putData("message", g.getRemainingCapacityMessage());
                    sendMessage(r);

                    TimestampUtil.printMessage(m.getUsername() + " - successfully started game " + m.getData("gameName") + ".");
                    break;
                } else if (m.getMessageType() == MessageType.JOINGAMEINFO) {
                    TimestampUtil.printMessage(m.getUsername() + " - wants to join a game called " + m.getData("gameName") + ".");

                    // Game not found
                    if (!GlobalServerThreads.doesGameRoomExist((String) m.getData("gameName"))) {
                        TimestampUtil.printMessage(m.getUsername() + " - failed to join a non-existent game. FOOL!");
                        Message r = new Message();
                        r.setMessageType(MessageType.JOINGAMEINFO);
                        r.putData("response", 0);
                        r.putData("message", "There is no game with name " + m.getData("gameName") + ".");
                        sendMessage(r);
                        break;
                    }

                    GameRoom g = GlobalServerThreads.addGameRoom(m, this);

                    // Game found but full
                    if (g == null) {
                        TimestampUtil.printMessage(m.getUsername() + " - " + m.getData("gameName") + " exists, but " + m.getUsername() + " is unable to join because the game is full.");
                        Message r = new Message();
                        r.setMessageType(MessageType.JOINGAMEINFO);
                        r.putData("response", 0);
                        r.putData("message", "Failed to join game: the game is full.");
                        sendMessage(r);
                        break;
                    }

                    Message stats = new Message();
                    stats.setMessageType(MessageType.OTHERPLAYERINFO);
                    stats.putData("username", m.getUsername());
                    Map<String, Integer> map = jdbc_server_client_Util.retrieveUserInfo(m.getUsername());
                    stats.putData("wins", map.get("wins"));
                    stats.putData("losses", map.get("losses"));
                    hs.broadcastExcludeSelf(stats, this);

                    Message r = new Message();
                    r.setMessageType(MessageType.JOINGAMEINFO);
                    r.putData("response", 1);
                    r.putData("message", g.getRemainingCapacityMessage());
                    hs.broadcast(r, this);

                    TimestampUtil.printMessage(m.getUsername() + " - successfully joined game " + m.getData("gameName") + ".");
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

    public void sendMessage(Message m) {
        try {
            oos.writeObject(m);
            oos.flush();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void run() {
        try {
            clientAuthentication();
            waitForClientToJoinRoom();
            while (true) {
                Message m = (Message)ois.readObject();
                if (m != null) {
                    hs.broadcast(m, this);
                } else {
                    System.out.println("NULL in HangmanServerThread:run()!");
                }
            }
        } catch (EOFException e) {
            TimestampUtil.printMessage("Client disconnected.");
        } catch (IOException | ClassNotFoundException e) {
            e.printStackTrace();
        } catch (AlreadyLoggedInException e) {
            TimestampUtil.printMessage(e.getMessage());
            sendDeauthPacket();
            interrupt();
        } finally {
            GlobalServerThreads.removeThread(username);
        }
    }

}
