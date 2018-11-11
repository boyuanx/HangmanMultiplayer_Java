package message;

public enum MessageType {
    HANDSHAKE("HANDSHAKE"),
    TEXT("TEXT"),
    NEWGAMECONFIG("NEWGAMECONFIG"),
    JOINGAMEINFO("JOINGAMEINFO");

    private final String text;

    MessageType(final String text) {
        this.text = text;
    }

    @Override
    public String toString() {
        return text;
    }
}
