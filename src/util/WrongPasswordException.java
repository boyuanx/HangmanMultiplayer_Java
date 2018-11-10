package util;

class WrongPasswordException extends Exception {
    WrongPasswordException() {
        super("The password you've entered does not match our records.");
    }
}