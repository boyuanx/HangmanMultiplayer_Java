package util;

import java.io.FilterInputStream;
import java.io.IOException;
import java.util.Scanner;

public class GlobalScanner {

    private static Scanner scanner = null;

    public static Scanner getScanner() {
        if (scanner == null) {
            scanner = new Scanner(new FilterInputStream(System.in) {
                @Override
                public void close() throws IOException {
                    //don't close System.in!
                }
            });
        }
        return scanner;
    }

    public static void reInit() {
        scanner.close();
        Scanner newScanner = new Scanner(new FilterInputStream(System.in) {
            @Override
            public void close() throws IOException {
                //don't close System.in!
            }
        });
        scanner = newScanner;
    }

}
