package util;

import client.GlobalSocket;
import message.Message;
import message.MessageType;

import java.io.IOException;
import java.sql.*;
import java.util.InputMismatchException;

public class jdbc_server_client_Util {

    private static Connection conn = null;
    private static String username = null;
    private static String password = null;
    private static int wins = 0;
    private static int losses = 0;
    private static boolean isAuthenticated = false;

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

    public static String userLogin() {
        while (true) {
            try {
                System.out.print("Username: ");
                String username = GlobalScanner.getScanner().nextLine();
                System.out.println();
                System.out.print("Password: ");
                String password = GlobalScanner.getScanner().nextLine();
                if (userAuth(username, password)) {
                    loginSuccessMessage();
                    return jdbc_server_client_Util.username;
                } else {
                    if (makeAccountFromCredentials(username, password)) {
                        return jdbc_server_client_Util.username;
                    }
                }
            } catch (WrongPasswordException e) {
                System.out.println(e.getMessage());
            }
        }
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

    private static boolean userAuth(String username, String password) throws WrongPasswordException {
        PreparedStatement ps;
        try {
            ps = conn.prepareStatement("SELECT * FROM Users WHERE username=?");
            ps.setString(1, username);
            ps.setString(1, password);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                jdbc_server_client_Util.isAuthenticated = true;
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
            jdbc_server_client_Util.isAuthenticated = true;

            return true;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    private static void loginSuccessMessage() {
        System.out.println("Great! You are now logged in as " + username +".");
        System.out.println();
        System.out.println(username + "'s Record");
        System.out.println("--------------");
        System.out.println("Wins - " + wins);
        System.out.println("Losses - " + losses);
        display_StartOrJoin();
    }

    private static void display_StartOrJoin() {
        System.out.println();
        System.out.println("    1) Start a Game");
        System.out.println("    2) Join a Game");
        System.out.println();
        System.out.print("Would you like to start a game or join a game? ");
        int i = getIntInput();
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
        makeNewGame(gameName, gameSize);
        System.out.println();
        System.out.println("Waiting for " + String.valueOf(gameSize-1) + " users to join...");
        System.out.println();
        System.out.println();
    }


    private static void makeNewGame(String gameName, int gameSize) {
        try {
            Message m = new Message(username);
            m.setMessageType(MessageType.NEWGAMECONFIG);
            m.putData("gameName", gameName);
            m.putData("gameSize", gameSize);
            GlobalSocket.oos.writeObject(m);
            GlobalSocket.oos.flush();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static void joinGame() {
        System.out.print("What is the name of the game? ");
        String gameName = GlobalScanner.getScanner().nextLine();
    }

    private static boolean yesNoParser(String s) {
        return s.equalsIgnoreCase("yes");
    }

    private static int getIntInput() {
        try {
            int i = GlobalScanner.getScanner().nextInt();
            GlobalScanner.getScanner().nextLine(); // To prevent Scanner from skipping
            return i;
        } catch (InputMismatchException e) {
            return 0;
        }
    }
}
