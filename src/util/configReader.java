package util;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Map;
import java.util.HashMap;
import java.util.Properties;

public class configReader {

    public static Map<String, String> promptUserForConfig(boolean isServer) {
        System.out.print("What is the name of the configuration file? ");
        String configFilename = GlobalScanner.getScanner().nextLine();
        return configReader.read(configFilename, isServer);
    }

    private static Map<String, String> read(String fileName, boolean isServer) {

        Map<String, String> map = new HashMap<>();
        Properties prop = new Properties();
        InputStream input = null;

        try {
            input = new FileInputStream(fileName);
            prop.load(input);
            System.out.println("Reading config file...");

            if (isServer) {
                if (prop.getProperty("ServerHostname") != null) {
                    map.put("ServerHostname", prop.getProperty("ServerHostname"));
                } else throw new JMConfigException("ServerHostname");

                if (prop.getProperty("ServerPort") != null) {
                    map.put("ServerPort", prop.getProperty("ServerPort"));
                } else throw new JMConfigException("ServerPort");

                if (prop.getProperty("DBConnection") != null) {
                    map.put("DBConnection", prop.getProperty("DBConnection"));
                } else throw new JMConfigException("DBConnection");

                if (prop.getProperty("DBUsername") != null) {
                    map.put("DBUsername", prop.getProperty("DBUsername"));
                } else throw new JMConfigException("DBUsername");

                if (prop.getProperty("DBPassword") != null) {
                    map.put("DBPassword", prop.getProperty("DBPassword"));
                } else throw new JMConfigException("DBPassword");

                if (prop.getProperty("SecretWordFile") != null) {
                    map.put("SecretWordFile", prop.getProperty("SecretWordFile"));
                } else throw new JMConfigException("SecretWordFile");
            } else {
                if (prop.getProperty("ServerHostname") != null) {
                    map.put("ServerHostname", prop.getProperty("ServerHostname"));
                } else throw new JMConfigException("ServerHostname");

                if (prop.getProperty("ServerPort") != null) {
                    map.put("ServerPort", prop.getProperty("ServerPort"));
                } else throw new JMConfigException("ServerPort");
            }




        } catch (IOException | JMConfigException e) {
            System.out.println(e.getMessage());
            map = null;
        } finally {
            if (input != null) {
                try {
                    input.close();
                } catch (IOException e) {
                    System.out.println(e.getMessage());
                }
            }
        }

        if (map != null) {
            if (isServer) {
                System.out.println("Database Connection String - " + map.get("DBConnection"));
                System.out.println("Database Username - " + map.get("DBUsername"));
                System.out.println("Database Password - " + map.get("DBPassword"));
                System.out.println("Secret Word File - " + map.get("SecretWordFile"));
                System.out.println("Server Hostname - " + map.get("ServerHostname"));
                System.out.println("Server Port - " + map.get("ServerPort"));
            } else {
                System.out.println("Server Hostname - " + map.get("ServerHostname"));
                System.out.println("Server Port - " + map.get("ServerPort"));
            }

            System.out.println();
        }

        if (isServer) {
            jdbc_server_client_Util.connect(map.get("DBConnection"), map.get("DBUsername"), map.get("DBPassword"));
        }

        return map;
    }

}
