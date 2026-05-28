package code.Exception;

public class MossaNonValidaException extends RuntimeException {

    public MossaNonValidaException() {
        super("Mossa non valida");
    }

    public MossaNonValidaException(String message) {
        super(message);
    }
    
}
