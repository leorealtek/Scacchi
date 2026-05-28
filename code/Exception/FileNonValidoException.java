package code.Exception;

public class FileNonValidoException extends RuntimeException {

    public FileNonValidoException() {
        super("File non valido");
    }

    public FileNonValidoException(String message) {
        super(message);
    }
}
