package client;

import message.Message;
import message.MessageType;
import util.GlobalScanner;
import util.jdbc_server_client_Util;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;

public class HangmanClient extends Thread {

    private String username;

    public void run() {
        // Multithreading here will make Scanner throw java.lang.IndexOutOfBoundsException and there is NO WAY to fix it.
        /*try {
            while (true) {
                Message m = (Message) GlobalSocket.ois.readObject();
                MessageType type = m.getMessageType();
                if (type == MessageType.OTHERPLAYERINFO) {
                    String username = (String)m.getData("username");
                    int wins = (int)m.getData("wins");
                    int losses = (int)m.getData("losses");
                    jdbc_server_client_Util.otherUserJoinedMessage(username, wins, losses);
                } else if (type == MessageType.JOINGAMEINFO) {
                    System.out.println();
                    System.out.println(m.getData("message"));
                } else if (type == MessageType.SERVERGAMERESPONSE) {
                    int response = (int)m.getData("response");
                    if (response == 0) {
                        System.out.println();
                        System.out.println(m.getData("message"));
                    } else if (response == 1) {
                        System.out.println();
                        System.out.println(m.getData("message"));
                        System.out.println();
                        System.out.println("You have " + m.getData("guessesRemaining") + " incorrect guesses remaining.");
                    }
                } else if (type == MessageType.WAIT) {
                    int shouldWait = (int)m.getData("shouldWait");
                    if (shouldWait == 0) {
                        String guess = jdbc_server_client_Util.promptUserToGuess();
                        jdbc_server_client_Util.sendGuessToServer(guess);
                    } else {
                        System.out.println("Waiting for " + m.getData("waitingForUser") + " to do something...");
                        System.out.println();
                    }
                }

                else {
                    System.err.println("Undefined type " + m.getMessageType() + ": " + m.getUsername() + ": " + m.getMessage());
                }
            }
        } catch (IOException | ClassNotFoundException e) {
            e.printStackTrace();
        }*/
    }

    HangmanClient(String hostname, int port) {
        try {
            System.out.print("Trying to connect to server...");
            GlobalSocket.s = new Socket(hostname, port);
            System.out.println("Connected!");
            initObjectStreams();
            jdbc_server_client_Util.userLogin();    // User login -> Send join game or new game messages
            username = jdbc_server_client_Util.getUsername();
            //this.start();
            //runListener();
            while (true) {
                Message m = (Message) GlobalSocket.ois.readObject();
                MessageType type = m.getMessageType();
                if (type == MessageType.OTHERPLAYERINFO) {
                    String username = (String)m.getData("username");
                    int wins = (int)m.getData("wins");
                    int losses = (int)m.getData("losses");
                    jdbc_server_client_Util.otherUserJoinedMessage(username, wins, losses);
                } else if (type == MessageType.JOINGAMEINFO) {
                    System.out.println();
                    System.out.println(m.getData("message"));
                } else if (type == MessageType.SERVERGAMERESPONSE) {
                    int response = (int)m.getData("response");
                    if (response == 0) {
                        System.out.println();
                        System.out.println(m.getData("message"));
                    } else if (response == 1) {
                        System.out.println();
                        System.out.println(m.getData("message"));
                        System.out.println();
                        System.out.println("You have " + m.getData("guessesRemaining") + " incorrect guesses remaining.");
                    }
                } else if (type == MessageType.WAIT) {
                    int shouldWait = (int)m.getData("shouldWait");
                    if (shouldWait == 0) {
                        String guess = jdbc_server_client_Util.promptUserToGuess();
                        jdbc_server_client_Util.sendGuessToServer(guess);
                    } else {
                        System.out.println();
                        System.out.println("Waiting for " + m.getData("waitingForUser") + " to do something...");
                    }
                } else if (type == MessageType.SERVEROTHERRESPONSE) {
                    System.out.println();
                    System.out.println(m.getData("message"));
                } else if (type == MessageType.WINSLOSSES) {

                } else if (type == MessageType.KILL) {

                }

                else {
                    System.err.println("Undefined type " + m.getMessageType() + ": " + m.getUsername() + ": " + m.getMessage());
                }
            }
        } catch (IOException e) {
            System.out.println("Unable to connect to server " + hostname + " on port " + port + ".");
            e.printStackTrace();
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }
        /*
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
        */
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
