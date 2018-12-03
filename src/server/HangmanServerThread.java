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
import java.net.SocketException;
import java.util.ArrayList;
import java.util.Map;
import java.util.Vector;

public class HangmanServerThread extends Thread {

    private ObjectInputStream ois;
    private ObjectOutputStream oos;
    private HangmanServer hs;
    public String username;
    private boolean isHost = false;

    HangmanServerThread(Socket s, HangmanServer hs) {
        try {
            ois = new ObjectInputStream(s.getInputStream());
            oos = new ObjectOutputStream(s.getOutputStream());
            this.hs = hs;
            start();
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
            this.username = username;
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
            m.putData("message", "Oops, it looks like you already have an active session! Bye bye get kicked!");
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
                        continue;
                    }
                    Message r = new Message();
                    r.setMessageType(MessageType.NEWGAMECONFIG);
                    r.putData("response", 1);
                    r.putData("message", g.getRemainingCapacityMessage());
                    sendMessage(r);

                    TimestampUtil.printMessage(m.getUsername() + " - successfully started game " + m.getData("gameName") + ".");
                    isHost = true;
                    hs.tryStartAllThreadsInRoom(g);
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
                        continue;
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
                        continue;
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
                    hs.broadcastGameRoom(r, this);

                    TimestampUtil.printMessage(m.getUsername() + " - successfully joined game " + m.getData("gameName") + ".");
                    hs.tryStartAllThreadsInRoom(g);
                    break;
                } else {
                    System.err.println("Expected handshake, received " + m.getMessageType() + " instead.");
                }
            }
        } catch (IOException e) {
            TimestampUtil.printMessage(username + " - Handshake with incoming client has failed: " + e.getMessage());
            TimestampUtil.printMessage(username + " - Thread killed.");
            stop();
        } catch (ClassNotFoundException e) {
            TimestampUtil.printMessage(username + " - Incoming client handshake corrupted: " + e.getMessage());
            TimestampUtil.printMessage(username + " - Thread killed.");
            stop();
        }
    }

    private void chooseNewSecretWord() {
        Message m = new Message();
        m.setMessageType(MessageType.SERVERGAMERESPONSE);
        m.putData("response", 0);
        m.putData("message", "Determining secret word...");
        hs.broadcastGameRoom(m, this);

        String secretWord = SecretWordUtil.chooseSecretWord();
        System.err.println(secretWord);
        GlobalServerThreads.setSecretWordForRoom(secretWord, this);
        String secretMessage = "Secret Word";
        for (char c : secretWord.toCharArray()) {
            secretMessage += " _";
        }

        Message n = new Message();
        n.setMessageType(MessageType.SERVERGAMERESPONSE);
        n.putData("response", 1);
        n.putData("message", secretMessage);
        n.putData("guessesRemaining", 7);
        hs.broadcastGameRoom(n, this);

        String currentUser = hs.getCurrentUserInRoom(this);
        Message go = new Message();
        go.setMessageType(MessageType.WAIT);
        go.putData("shouldWait", 0);
        hs.broadcastOnlyUser(go, currentUser, this, true);

        Message wait = new Message();
        wait.setMessageType(MessageType.WAIT);
        wait.putData("shouldWait", 1);
        wait.putData("waitingForUser", currentUser);
        hs.broadcastExcludeUser(m, currentUser, this);
    }

    private void broadcastWordAndWait(boolean init) {

    }

    private void processClientGuess(Message m) {
        if (m.getMessageType() == MessageType.CLIENTGAMERESPONSE) {
            int isLetterGuess = (int)m.getData("isLetterGuess");
            String guess = (String)m.getData("guess");
            boolean result;
            if (isLetterGuess == 0) {
                result = GlobalServerThreads.checkWordGuessForRoom(guess, this);
                if (result) {
                    GameRoom g = GlobalServerThreads.findGameRoom(this);
                    for (String username : g.getClientThreads().keySet()) {
                        if (username != this.username) {
                            jdbc_server_client_Util.updateWinLoss(username, false);
                        } else {
                            jdbc_server_client_Util.updateWinLoss(this.username, true);
                        }
                    }
                    hs.broadcastWinLossKillToRoom(this);
                } else {

                }
            } else {
                result = GlobalServerThreads.checkIfLetterInWordForRoom(guess, this);
            }

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

    public void startGame() {
        if (isHost) {
            chooseNewSecretWord();
            Vector<GameRoom> gameRooms = GlobalServerThreads.gameRooms;
        }
        try {
            while (true) {
                Vector<GameRoom> gameRooms = GlobalServerThreads.gameRooms;
                Message m = (Message) ois.readObject();
                processClientGuess(m);
            }
        } catch (EOFException | SocketException e) {
            TimestampUtil.printMessage(username + " - disconnected.");
        } catch (IOException | ClassNotFoundException e) {
            e.printStackTrace();
        } finally {
            GlobalServerThreads.removeThread(username);
            GlobalServerThreads.removeClientFromRooms(username);
        }
            /*
            if (m != null) {
                hs.broadcastGameRoom(m, this);
            }
            */

    }

    public void run() {
        try {
            clientAuthentication();
            waitForClientToJoinRoom();
            Vector<GameRoom> gameRooms = GlobalServerThreads.gameRooms;

        } catch (AlreadyLoggedInException e) {
            TimestampUtil.printMessage(e.getMessage());
            sendDeauthPacket();
            stop();
        }
    }

}
