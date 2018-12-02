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

    // Data keys for NEWGAMECONFIG: String gameName, int gameSize
    // Server response for NEWGAMECONFIG: int response, String message
    // 1 = Success, 0 = Failure
    NEWGAMECONFIG("NEWGAMECONFIG"),

    // Data keys for JOINGAMEINFO: String gameName
    // Server response for JOINGAMEINFO: int response, String message
    // 1 = Success, 0 = Failure
    JOINGAMEINFO("JOINGAMEINFO"),

    // Receive-only message type
    // Data keys for OTHERPLAYERINFO: String username, String wins, String losses
    OTHERPLAYERINFO("OTHERPLAYERINFO"),

    // Data keys for SERVERGAMERESPONSE: int response, String message, int guessesRemaining
    // 0 = Determining secret word, 1 = Word printout + guesses remaining
    SERVERGAMERESPONSE("SERVERGAMERESPONSE"),

    // Data keys for WAIT: int shouldWait, String waitingForUser
    WAIT("WAIT"),

    // Data keys for CLIENTGAMERESPONSE: int isLetterGuess, String guess
    CLIENTGAMERESPONSE("CLIENTGAMERESPONSE"),

    // Data keys for SERVEROTHERRESPONSE: String message
    SERVEROTHERRESPONSE("SERVEROTHERRESPONSE"),

    // Server-side kill switch for client: String message
    KILL("KILL");

    private final String text;

    MessageType(final String text) {
        this.text = text;
    }

    @Override
    public String toString() {
        return text;
    }
}
