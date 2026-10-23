package portal.attendance.exception;

import org.springframework.http.HttpStatus;

/**
 * Ошибка «ресурс не найден» в реактивном attendance-service.
 */
public class ResourceNotFoundException extends RuntimeException {

    private final HttpStatus status = HttpStatus.NOT_FOUND;

    public ResourceNotFoundException(String message) {
        super(message);
    }

    public HttpStatus getStatus() {
        return status;
    }
}
