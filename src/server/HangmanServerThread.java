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
import java.util.Map;

public class HangmanServerThread extends Thread {

    private ObjectInputStream ois;
    private ObjectOutputStream oos;
    private HangmanServer hs;
    public String username;
    private boolean isHost = false;
    private boolean lastGuessSuccess = false;

    HangmanServerThread(Socket s, HangmanServer hs) {
        try {
            ois = new ObjectInputStream(s.getInputStream());
            oos = new ObjectOutputStream(s.getOutputStream());
            this.hs = hs;
            clientAuthentication();
            waitForClientToJoinRoom();
            //start();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (AlreadyLoggedInException e) {
            TimestampUtil.printMessage(e.getMessage());
            sendDeauthPacket();
            stop();
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
                    response.putData("wins", jdbc_server_client_Util.getWins());
                    response.putData("losses", jdbc_server_client_Util.getLosses());
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

                    TimestampUtil.printMessage(m.getUsername() + " - successfully created game " + m.getData("gameName") + ".");
                    isHost = true;
                    hs.tryStartAllThreadsInRoom(g, this);
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
                        TimestampUtil.printMessage(m.getUsername() + " - " + m.getData("gameName") + " exists, but " + m.getUsername() + " is unable to join because the maximum number of players have already joined " + m.getData("gameName") + ".");
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
                    hs.tryStartAllThreadsInRoom(g, this);
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
        GameRoom g = GlobalServerThreads.findGameRoom(this);
        TimestampUtil.printMessage(username + " - " + g.getGameName() + " has " + g.getGameSize() + " players so starting game. Secret word is " + secretWord + ".");
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
        hs.broadcastExcludeUser(wait, currentUser, this);
    }

    private void continueGame() {
        String secretWord = GlobalServerThreads.getSecretWordFromRoom(this);
        String secretWordMutable = GlobalServerThreads.getSecretWordMutableFromRoom(this);
        String secretMessage = "Secret Word";

        for (int i = 0; i < secretWord.length(); i++) {
            if (secretWordMutable.charAt(i) == '~') {
                secretMessage += " " + String.valueOf(secretWord.charAt(i));
            } else {
                secretMessage += " _";
            }
        }

        GameRoom g = GlobalServerThreads.findGameRoom(this);

        if (lastGuessSuccess) {
            TimestampUtil.printMessage(g.getGameName() + " " + username + " - " + secretMessage + ".");
        } else {
            TimestampUtil.printMessage(g.getGameName() + " now has " + g.guessesLeft + " guesses remaining.");
        }

        Message n = new Message();
        n.setMessageType(MessageType.SERVERGAMERESPONSE);
        n.putData("response", 1);
        n.putData("message", secretMessage);
        n.putData("guessesRemaining", g.guessesLeft);
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
        hs.broadcastExcludeUser(wait, currentUser, this);
    }

    private void processClientGuess(Message m) {
        if (m.getMessageType() == MessageType.CLIENTGAMERESPONSE) {
            int isLetterGuess = (int)m.getData("isLetterGuess");
            String guessSender = m.getUsername();
            String guess = (String)m.getData("guess");
            GameRoom g = GlobalServerThreads.findGameRoom(this);
            boolean result;
            if (isLetterGuess == 0) {
                TimestampUtil.printMessage(g.getGameName() + " " + username + " - guessed word " + guess + ".");
                result = GlobalServerThreads.checkWordGuessForRoom(guess, this);
                if (result) {
                    TimestampUtil.printMessage(g.getGameName() + " " + username + " - " + guess + " is correct. " + username + " wins the game.");
                    for (String username : g.getClientThreads().keySet()) {
                        if (username.equals(this.username)) {
                            jdbc_server_client_Util.updateWinLoss(this.username, true);
                        } else {
                            jdbc_server_client_Util.updateWinLoss(username, false);
                        }
                    }
                    hs.broadcastWinLossKillToRoom(this);
                } else {
                    TimestampUtil.printMessage(g.getGameName() + " " + username + " - " + guess + " is not correct. " + username + " has lost and is no longer in the game.");
                    for (String username : g.getClientThreads().keySet()) {
                        if (username.equals(this.username)) {
                            hs.broadcastDisconnectToUser(this);
                        } else {
                            hs.broadcastOtherUserDisconnect(g.getClientThreads().get(username));
                        }
                    }
                    g.guessesLeft--;
                }
            } else {
                TimestampUtil.printMessage(g.getGameName() + " " + username + " - guessed letter " + guess + ".");
                // Tommy has guessed letter 'a'.
                Message k = new Message();
                k.setMessageType(MessageType.SERVEROTHERRESPONSE);
                k.putData("message", guessSender + " has guessed letter " + guess + ".");
                hs.broadcastExcludeUser(k, guessSender, this);

                result = GlobalServerThreads.checkIfLetterInWordForRoom(guess, this);
                if (result) {
                    lastGuessSuccess = true;
                    Message t = new Message();
                    t.setMessageType(MessageType.SERVEROTHERRESPONSE);
                    t.putData("message", "The letter '" + guess + "' is in the secret word.");
                    hs.broadcastGameRoom(t, this);
                    TimestampUtil.printMessage(g.getGameName() + " " + username + " - " + guess + " is in the secret word.");
                } else {
                    Message t = new Message();
                    t.setMessageType(MessageType.SERVEROTHERRESPONSE);
                    t.putData("message", "The letter '" + guess + "' is not in the secret word.");
                    hs.broadcastGameRoom(t, this);
                }
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
        }
        try {
            while (true) {
                Message m = (Message) ois.readObject();
                processClientGuess(m);
                continueGame();
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
        startGame();
    }

}
