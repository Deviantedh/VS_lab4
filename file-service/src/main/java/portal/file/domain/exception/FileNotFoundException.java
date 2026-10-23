package portal.file.domain.exception;

public class FileNotFoundException extends RuntimeException {
    public FileNotFoundException(Long id) {
        super("Файл с ID " + id + " не найден");
    }

    public FileNotFoundException(String message) {
        super(message);
    }
}
