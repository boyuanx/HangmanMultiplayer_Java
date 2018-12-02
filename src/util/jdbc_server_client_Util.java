package util;

import client.GlobalSocket;
import message.Message;
import message.MessageType;
import server.GlobalServerThreads;

import java.io.IOException;
import java.sql.*;
import java.util.HashMap;
import java.util.InputMismatchException;
import java.util.Map;

public class jdbc_server_client_Util {

    private static Connection conn = null;
    private static String username = null;
    private static String password = null;
    private static int wins = 0;
    private static int losses = 0;

    static void connect(String DBConnection, String DBUsername, String DBPassword) {
        Connection conn = null;
        String jdbc_creds = DBConnection + "?user=" + DBUsername + "&password=" + DBPassword + "&useSSL=false&useLegacyDatetimeCode=false&serverTimezone=UTC";
        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
            System.out.print("Trying to connect to database... ");
            conn = DriverManager.getConnection(jdbc_creds);
            System.out.println("Connected!");
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        } catch (SQLException e) {
            System.out.println("Unable to connect to database " + DBConnection + " with username " + DBUsername + " and password " + DBPassword + ".");
        }
        jdbc_server_client_Util.conn = conn;
    }

    public static void userLogin() {
        while (true) {
            try {
                System.out.println();
                System.out.print("Username: ");
                String username = GlobalScanner.getScanner().nextLine();
                jdbc_server_client_Util.username = username;
                System.out.print("Password: ");
                String password = GlobalScanner.getScanner().nextLine();
                jdbc_server_client_Util.password = password;

                Message m = new Message();
                m.setMessageType(MessageType.AUTHENTICATION);
                m.putData("username", username);
                m.putData("password", password);

                GlobalSocket.oos.writeObject(m);
                GlobalSocket.oos.flush();

                getAuthResponse();
                break;
            } catch (IOException | ClassNotFoundException | WrongPasswordException | RefuseToCreateAccountEdgyException e) {
                System.err.println(e.getMessage());
            }
        }
    }

    private static void getAuthResponse() throws IOException, ClassNotFoundException, WrongPasswordException, RefuseToCreateAccountEdgyException {
        Message m = (Message) GlobalSocket.ois.readObject();
        MessageType type = m.getMessageType();
        if (type == MessageType.AUTHENTICATION) {
            int response = (int) m.getData("response");
            if (response == 1) {
                jdbc_server_client_Util.loginSuccessMessage();
                username = jdbc_server_client_Util.getUsername();
            } else if (response == 0) {
                throw new WrongPasswordException();
            } else if (response == -1) {
                if (jdbc_server_client_Util.makeAccountFromCredentials()) {
                    username = jdbc_server_client_Util.getUsername();
                } else {
                    throw new RefuseToCreateAccountEdgyException();
                }
            } else if (response == -2) {
                System.err.println(m.getData("message"));
                System.exit(7);
            }
        }
    }

    private static boolean makeAccountFromCredentials() {
        return makeAccountFromCredentials(username, password);
    }

    private static boolean makeAccountFromCredentials(String username, String password) {
        System.out.println();
        System.out.println("No account exists with those credentials.");
        System.out.print("Would you like to create a new account? (Yes/No): ");
        if (!yesNoParser(GlobalScanner.getScanner().nextLine())) {
            return false;
        }
        System.out.println("Would you like to use the username and password above? (Yes/No): ");
        if (!yesNoParser(GlobalScanner.getScanner().nextLine())) {
            return false;
        }
        if (makeAccount(username, password)) {
            loginSuccessMessage();
            return true;
        } else {
            System.out.println("Something went wrong when creating your account.");
            return false;
        }
    }

    public static boolean userAuth(String username, String password) throws WrongPasswordException {
        PreparedStatement ps;
        try {
            ps = conn.prepareStatement("SELECT * FROM Users WHERE username=?");
            ps.setString(1, username);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                jdbc_server_client_Util.username = rs.getString("username");
                String userPassword = rs.getString("password");
                if (!userPassword.equals(password)) {
                    throw new WrongPasswordException();
                }
                jdbc_server_client_Util.password = userPassword;
                jdbc_server_client_Util.wins = rs.getInt("wins");
                jdbc_server_client_Util.losses = rs.getInt("losses");
                rs.close();
                ps.close();
                return true;
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    private static boolean makeAccount(String username, String password) {
        try {
            Message m = new Message(username);
            m.setMessageType(MessageType.MAKEACCOUNT);
            m.putData("username", username);
            m.putData("password", password);
            GlobalSocket.oos.writeObject(m);
            GlobalSocket.oos.flush();

            Message r = (Message)GlobalSocket.ois.readObject();
            if (r.getMessageType() == MessageType.MAKEACCOUNT) {
                int response = (int)r.getData("response");
                if (response == 1) {
                    wins = (int)r.getData("wins");
                    losses = (int)r.getData("losses");
                    return true;
                } else if (response == 0) {
                    System.err.println("Error making an account. Please contact your server administrator.");
                }
            } else {
                System.err.println("Expected a MAKEACCOUNT message but received " + r.getMessageType() + ".");
            }
        } catch (IOException e) {
            System.err.println("Error sending request to make a new account.");
        } catch (ClassNotFoundException e) {
            System.err.println("Error receiving server response for making a new account. Possible stream corruption?");
        }
        return false;
    }

    public static boolean serverMakeAccount(String username, String password) {
        PreparedStatement ps;
        try {
            ps = conn.prepareStatement("INSERT INTO Users (username, password, wins, losses) VALUES (?, ?, ?, ?)");
            ps.setString(1, username);
            ps.setString(2, password);
            ps.setInt(3, 0);
            ps.setInt(4, 0);
            ps.execute();
            ps.close();

            jdbc_server_client_Util.username = username;
            jdbc_server_client_Util.password = password;
            jdbc_server_client_Util.wins = 0;
            jdbc_server_client_Util.losses = 0;

            return true;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    public static void loginSuccessMessage() {
        System.out.println();
        System.out.println("Great! You are now logged in as " + username +".");
        System.out.println();
        System.out.println(username + "'s Record");
        System.out.println("--------------");
        System.out.println("Wins - " + wins);
        System.out.println("Losses - " + losses);
        display_StartOrJoin();
    }

    public static void otherUserJoinedMessage(String username, int wins, int losses) {
        System.out.println();
        System.out.println("User " + username + " is in the game.");
        System.out.println();
        System.out.println(username + "'s Record");
        System.out.println("--------------");
        System.out.println("Wins - " + wins);
        System.out.println("Losses - " + losses);
    }

    private static void display_StartOrJoin() {
        System.out.println();
        System.out.println("    1) Start a Game");
        System.out.println("    2) Join a Game");
        System.out.println();
        System.out.print("Would you like to start a game or join a game? ");
        int i = getIntInput();
        System.out.println();
        if (i == 1) {
            startNewGame();
        } else if (i == 2) {
            joinGame();
        } else {
            System.out.println("Invalid selection!");
            display_StartOrJoin();
        }
    }

    private static void startNewGame() {
        System.out.print("What is the name of the game? ");
        String gameName = GlobalScanner.getScanner().nextLine();
        System.out.println();
        System.out.print("How many users will be playing (1-4)? ");
        int gameSize = getIntInput();
        System.out.println();

        if (gameSize < 1 || gameSize > 4) {
            System.out.println("A game can only have between 1-4 players...");
            System.out.println();
            startNewGame();
        }

        try {
            Message m = new Message(username);
            m.setMessageType(MessageType.NEWGAMECONFIG);
            m.putData("gameName", gameName);
            m.putData("gameSize", gameSize);
            GlobalSocket.oos.writeObject(m);
            GlobalSocket.oos.flush();

            Message r = (Message)GlobalSocket.ois.readObject();
            System.out.println(r.getData("message"));
            int response = (int)r.getData("response");
            if (response == 0) {
                System.out.println();
                startNewGame();
            }

        } catch (IOException | ClassNotFoundException e) {
            e.printStackTrace();
        }

        System.out.println();
        System.out.println();
    }

    private static void joinGame() {
        System.out.print("What is the name of the game? ");
        String gameName = GlobalScanner.getScanner().nextLine();

        try {
            Message m = new Message(username);
            m.setMessageType(MessageType.JOINGAMEINFO);
            m.putData("gameName", gameName);
            GlobalSocket.oos.writeObject(m);
            GlobalSocket.oos.flush();

            Message r = (Message)GlobalSocket.ois.readObject();
            System.out.println(r.getData("message"));
            int response = (int)r.getData("response");
            if (response == 0) {
                System.out.println();
                joinGame();
            }
        } catch (IOException | ClassNotFoundException e) {
            e.printStackTrace();
        }

    }

    public static boolean getNewJoinGameResponse() {
        try {
            Message m = (Message) GlobalSocket.ois.readObject();
            MessageType type = m.getMessageType();

            if (type != MessageType.JOINGAMEINFO && type != MessageType.NEWGAMECONFIG) {
                System.err.println("Wrong message type received from server. Actual type: " + type + ".");
                return false;
            }

            int response = (int)m.getData("response");
            if (response == 1) {
                System.out.println(m.getData("message"));
                return true;
            } else {
                System.err.println(m.getData("message"));
                return false;
            }

        } catch (IOException | ClassNotFoundException e) {
            e.printStackTrace();
        }
        return false;
    }

    public static Map<String, Integer> retrieveUserInfo(String username) {
        Map<String, Integer> map = new HashMap<>();
        PreparedStatement ps;
        try {
            ps = conn.prepareStatement("SELECT * FROM Users WHERE username=?");
            ps.setString(1, username);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                map.put("wins", rs.getInt("wins"));
                map.put("losses", rs.getInt("losses"));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return map;
    }

    private static boolean yesNoParser(String s) {
        return s.equalsIgnoreCase("yes");
    }

    private static int getIntInput() {
        try {
            return Integer.parseInt(GlobalScanner.getScanner().nextLine());
        } catch (InputMismatchException e) {
            GlobalScanner.getScanner().nextLine();
            return 0;
        }
    }

    public static String getUsername() {
        return username;
    }

    public static int getWins() {
        return wins;
    }

    public static int getLosses() {
        return losses;
    }
}
