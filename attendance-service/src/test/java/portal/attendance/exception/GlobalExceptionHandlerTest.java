package portal.attendance.exception;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.server.ServerWebInputException;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import static org.assertj.core.api.Assertions.assertThat;

class GlobalExceptionHandlerTest {

    private GlobalExceptionHandler handler;

    @BeforeEach
    void setUp() {
        handler = new GlobalExceptionHandler();
    }

    @Test
    @DisplayName("handleNotFound returns 404 NOT_FOUND with ErrorResponse body")
    void testHandleNotFound() {
        ResourceNotFoundException ex = new ResourceNotFoundException("Запись о явке с ID 123 не найдена");

        Mono<ResponseEntity<ErrorResponse>> resultMono = handler.handleNotFound(ex);

        StepVerifier.create(resultMono)
                .assertNext(response -> {
                    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
                    assertThat(response.getBody()).isNotNull();
                    assertThat(response.getBody().getStatus()).isEqualTo(404);
                    assertThat(response.getBody().getError()).isEqualTo("Not Found");
                    assertThat(response.getBody().getMessage()).isEqualTo("Запись о явке с ID 123 не найдена");
                    assertThat(response.getBody().getTimestamp()).isNotNull();
                })
                .verifyComplete();
    }

    @Test
    @DisplayName("handleValidation returns 400 BAD_REQUEST for validation exception")
    void testHandleValidation() {
        Exception ex = new RuntimeException("Validation error");

        Mono<ResponseEntity<ErrorResponse>> resultMono = handler.handleValidation(ex);

        StepVerifier.create(resultMono)
                .assertNext(response -> {
                    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
                    assertThat(response.getBody()).isNotNull();
                    assertThat(response.getBody().getStatus()).isEqualTo(400);
                    assertThat(response.getBody().getError()).isEqualTo("Bad Request");
                    assertThat(response.getBody().getMessage()).contains("Параметры запроса не соответствуют ограничениям");
                    assertThat(response.getBody().getTimestamp()).isNotNull();
                })
                .verifyComplete();
    }

    @Test
    @DisplayName("handleInput returns 400 BAD_REQUEST for malformed request")
    void testHandleInput() {
        ServerWebInputException ex = new ServerWebInputException("Invalid format");

        Mono<ResponseEntity<ErrorResponse>> resultMono = handler.handleInput(ex);

        StepVerifier.create(resultMono)
                .assertNext(response -> {
                    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
                    assertThat(response.getBody()).isNotNull();
                    assertThat(response.getBody().getStatus()).isEqualTo(400);
                    assertThat(response.getBody().getError()).isEqualTo("Bad Request");
                    assertThat(response.getBody().getMessage()).contains("Некорректный запрос");
                    assertThat(response.getBody().getTimestamp()).isNotNull();
                })
                .verifyComplete();
    }

    @Test
    @DisplayName("handleUnexpected returns 500 INTERNAL_SERVER_ERROR for unhandled exception")
    void testHandleUnexpected() {
        Exception ex = new RuntimeException("Database failure");

        Mono<ResponseEntity<ErrorResponse>> resultMono = handler.handleUnexpected(ex);

        StepVerifier.create(resultMono)
                .assertNext(response -> {
                    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
                    assertThat(response.getBody()).isNotNull();
                    assertThat(response.getBody().getStatus()).isEqualTo(500);
                    assertThat(response.getBody().getError()).isEqualTo("Internal Server Error");
                    assertThat(response.getBody().getMessage()).contains("Внутренняя ошибка сервиса");
                    assertThat(response.getBody().getTimestamp()).isNotNull();
                })
                .verifyComplete();
    }
}
