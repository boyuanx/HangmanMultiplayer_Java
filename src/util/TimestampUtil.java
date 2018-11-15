package util;

import java.util.Date;

public class TimestampUtil {

    public static void printMessage(String s) {
        System.err.println(new java.text.SimpleDateFormat("HH:mm:ss.SSS").format(new Date()) + ": " + s);
    }

}
