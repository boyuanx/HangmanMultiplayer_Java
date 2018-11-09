package util;

public class JMConfigException extends Exception {
    public JMConfigException(String s) {
        super(s + " is a required parameter in the configuration file.");
    }
}
