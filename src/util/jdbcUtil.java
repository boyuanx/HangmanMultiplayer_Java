package util;

import java.sql.*;
import java.util.Scanner;

public class jdbcUtil {

    public static Connection conn = null;
    private static String username = null;
    private static String password = null;
    private static int wins = 0;
    private static int losses = 0;
    private static boolean isAuthenticated = false;

    public static Connection connect(String DBConnection, String DBUsername, String DBPassword) {
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
        jdbcUtil.conn = conn;
        return conn;
    }

    public static boolean userLogin() {
        System.out.print("Username: ");
        String username = GlobalScanner.getScanner().nextLine();
        System.out.println();
        System.out.print("Password: ");
        String password = GlobalScanner.getScanner().nextLine();
        GlobalScanner.getScanner().close();
        return userAuth(username, password);
    }

    public static boolean noAccountExists() {
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
            System.out.println("Great! You are now logged in as " + username +".");
            return true;
        } else {
            System.out.println("Something went wrong when creating your account.");
            return false;
        }
    }

    private static boolean userAuth(String username, String password) {
        PreparedStatement ps;
        try {
            ps = conn.prepareStatement("SELECT * FROM Users WHERE username=? AND password=?");
            ps.setString(1, username);
            ps.setString(1, password);
            ResultSet rs = ps.executeQuery();
            ps.close();
            if (rs.next()) {
                jdbcUtil.isAuthenticated = true;
                jdbcUtil.username = rs.getString("username");
                jdbcUtil.password = rs.getString("password");
                jdbcUtil.wins = rs.getInt("wins");
                jdbcUtil.losses = rs.getInt("losses");
                rs.close();
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
            ps = conn.prepareStatement("INSERT INTO User (username, password, wins, losses) VALUES (?, ?, ?, ?)");
            ps.setString(1, username);
            ps.setString(2, password);
            ps.setInt(3, 0);
            ps.setInt(4, 0);
            ps.execute();
            ps.close();

            jdbcUtil.username = username;
            jdbcUtil.password = password;
            jdbcUtil.wins = 0;
            jdbcUtil.losses = 0;
            jdbcUtil.isAuthenticated = true;

            return true;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    public static boolean yesNoParser(String s) {
        String ss = s.toLowerCase();
        if (s == "yes") {
            return true;
        } else {
            return false;
        }
    }
}
