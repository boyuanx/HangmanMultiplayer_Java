package server;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Stream;

public class SecretWordUtil {

    private static String secretWordFileName;
    private static ArrayList<String> wordList;

    public static void initWordList(String name) {
        setFileName(name);
        readSecretWordFile();
    }

    private static void setFileName(String name) {
        secretWordFileName = name;
    }

    private static ArrayList<String> readSecretWordFile() {
        if (secretWordFileName == null) {
            return null;
        }

        ArrayList<String> result = new ArrayList<>();

        try (Stream<String> stream = Files.lines(Paths.get(secretWordFileName))) {
            stream.forEach(result::add);
        } catch (IOException e) {
            e.printStackTrace();
        }

        return result;
    }

    public static String chooseSecretWord() {
        if (wordList == null) {
            return null;
        }
        int randomIndex = ThreadLocalRandom.current().nextInt(0, wordList.size());
        return wordList.get(randomIndex);
    }

}