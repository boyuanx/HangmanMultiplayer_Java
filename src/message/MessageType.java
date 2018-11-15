package message;

public enum MessageType {
    // Data keys for AUTHENTICATION: String username, String password
    // Server response for AUTHENTICATION: int response
    // int 1 == Success, int 0 == Wrong password, int -1 == No account exists
    AUTHENTICATION("AUTHENTICATION"),

    // Data keys for MAKEACCOUNT: String username, String password
    // Server response for MAKEACCOUNT: int response
    // 1 = Success, 0 = Failure
    // If response == 1: String username, String password
    MAKEACCOUNT("MAKEACCOUNT"),

    // Data keys for LETTERGUESS: String letter
    LETTERGUESS("LETTERGUESS"),

    // Data keys for WORDGUESS: String word
    WORDGUESS("WORDGUESS"),

    // Data keys for NEWGAMECONFIG: String gameName, int gameSize
    // Server response for NEWGAMECONFIG: int response, String message
    // 1 = Success, 0 = Failure
    NEWGAMECONFIG("NEWGAMECONFIG"),

    // Data keys for JOINGAMEINFO: String gameName
    // Server response for JOINGAMEINFO: int response, String message
    // 1 == Success, 0 == Failure
    JOINGAMEINFO("JOINGAMEINFO"),

    // Data keys for SERVERGAMERESPONSE: String secretWord, int guessesRemaining, int isPreviousGuessInSecretWord, String previousGuess
    SERVERGAMERESPONSE("SERVERRESPONSE"),

    // Data keys for SERVEROTHERRESPONSE: String response
    SERVEROTHERRESPONSE("SERVEROTHERRESPONSE");

    private final String text;

    MessageType(final String text) {
        this.text = text;
    }

    @Override
    public String toString() {
        return text;
    }
}
