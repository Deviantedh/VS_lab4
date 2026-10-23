package portal.attendance.exception;

import java.time.Instant;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

/**
 * Тело ошибки в человеко читаемом формате.
 */
@Getter
@Builder
@AllArgsConstructor
public class ErrorResponse {

    private Instant timestamp;
    private int status;
    private String error;
    private String message;
}
