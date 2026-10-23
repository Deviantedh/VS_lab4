package portal.schedule.exception;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import jakarta.validation.Path;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.core.PropertyReferenceException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class GlobalExceptionHandlerTest {

    private GlobalExceptionHandler exceptionHandler;

    @Mock
    private HttpServletRequest request;

    @BeforeEach
    void setUp() {
        exceptionHandler = new GlobalExceptionHandler();
        lenient().when(request.getRequestURI()).thenReturn("/api/test");
    }

    @Test
    @DisplayName("Handle ResourceNotFoundException returns 404")
    void testHandleNotFound() {
        ResourceNotFoundException ex = new ResourceNotFoundException("Entity with id 42 not found");
        ResponseEntity<ErrorResponse> response = exceptionHandler.handleNotFound(ex, request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getStatus()).isEqualTo(404);
        assertThat(response.getBody().getError()).isEqualTo("Not Found");
        assertThat(response.getBody().getMessage()).contains("Entity with id 42 not found");
        assertThat(response.getBody().getPath()).isEqualTo("/api/test");
    }

    @Test
    @DisplayName("Handle NoResourceFoundException returns 404")
    void testHandleNoResourceFound() {
        NoResourceFoundException ex = mock(NoResourceFoundException.class);
        when(ex.getMessage()).thenReturn("Resource missing");

        ResponseEntity<ErrorResponse> response = exceptionHandler.handleNoResourceFound(ex, request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getStatus()).isEqualTo(404);
        assertThat(response.getBody().getMessage()).contains("/api/test");
    }

    @Test
    @DisplayName("Handle BusinessConflictException returns 409")
    void testHandleConflict() {
        BusinessConflictException ex = new BusinessConflictException("Shift overlaps with vacation");
        ResponseEntity<ErrorResponse> response = exceptionHandler.handleConflict(ex, request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getStatus()).isEqualTo(409);
        assertThat(response.getBody().getError()).isEqualTo("Conflict");
        assertThat(response.getBody().getMessage()).isEqualTo("Shift overlaps with vacation");
    }

    @Test
    @DisplayName("Handle MethodArgumentNotValidException returns 400 with validation errors")
    void testHandleValidation() {
        MethodArgumentNotValidException ex = mock(MethodArgumentNotValidException.class);
        BindingResult bindingResult = mock(BindingResult.class);
        FieldError fieldError = new FieldError("object", "email", "Email is required");

        when(ex.getBindingResult()).thenReturn(bindingResult);
        when(bindingResult.getFieldErrors()).thenReturn(List.of(fieldError));

        ResponseEntity<ErrorResponse> response = exceptionHandler.handleValidation(ex, request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getValidationErrors()).containsEntry("email", "Email is required");
    }

    @Test
    @DisplayName("Handle ConstraintViolationException returns 400")
    void testHandleConstraintViolation() {
        @SuppressWarnings("unchecked")
        ConstraintViolation<Object> violation = mock(ConstraintViolation.class);
        Path path = mock(Path.class);
        when(path.toString()).thenReturn("page");
        when(violation.getPropertyPath()).thenReturn(path);
        when(violation.getMessage()).thenReturn("must be greater than 0");

        ConstraintViolationException ex = new ConstraintViolationException(Set.of(violation));
        ResponseEntity<ErrorResponse> response = exceptionHandler.handleConstraintViolation(ex, request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getValidationErrors()).containsEntry("page", "must be greater than 0");
    }

    @Test
    @DisplayName("Handle HandlerMethodValidationException returns 400")
    void testHandleMethodValidation() {
        HandlerMethodValidationException ex = mock(HandlerMethodValidationException.class);
        ResponseEntity<ErrorResponse> response = exceptionHandler.handleMethodValidation(ex, request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getMessage()).contains("Параметры запроса не соответствуют ограничениям");
    }

    @Test
    @DisplayName("Handle HttpMessageNotReadableException returns 400")
    void testHandleMessageNotReadable() {
        HttpMessageNotReadableException ex = mock(HttpMessageNotReadableException.class);
        ResponseEntity<ErrorResponse> response = exceptionHandler.handleMessageNotReadable(ex, request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getMessage()).contains("Некорректный формат тела запроса");
    }

    @Test
    @DisplayName("Handle MethodArgumentTypeMismatchException returns 400")
    void testHandleTypeMismatch() {
        MethodArgumentTypeMismatchException ex = mock(MethodArgumentTypeMismatchException.class);
        when(ex.getName()).thenReturn("id");
        when(ex.getValue()).thenReturn("abc");

        ResponseEntity<ErrorResponse> response = exceptionHandler.handleTypeMismatch(ex, request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getMessage()).contains("id").contains("abc");
    }

    @Test
    @DisplayName("Handle DataIntegrityViolationException returns 409")
    void testHandleDataIntegrity() {
        DataIntegrityViolationException ex = new DataIntegrityViolationException("Unique key violation");
        ResponseEntity<ErrorResponse> response = exceptionHandler.handleDataIntegrity(ex, request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getError()).isEqualTo("Conflict");
    }

    @Test
    @DisplayName("Handle IllegalArgumentException returns 400")
    void testHandleIllegalArgument() {
        IllegalArgumentException ex = new IllegalArgumentException("Invalid argument value");
        ResponseEntity<ErrorResponse> response = exceptionHandler.handleIllegalArgument(ex, request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getMessage()).isEqualTo("Invalid argument value");
    }

    @Test
    @DisplayName("Handle HttpRequestMethodNotSupportedException returns 405")
    void testHandleMethodNotSupported() {
        HttpRequestMethodNotSupportedException ex = mock(HttpRequestMethodNotSupportedException.class);
        when(ex.getMethod()).thenReturn("PATCH");

        ResponseEntity<ErrorResponse> response = exceptionHandler.handleMethodNotSupported(ex, request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.METHOD_NOT_ALLOWED);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getMessage()).contains("PATCH");
    }

    @Test
    @DisplayName("Handle PropertyReferenceException returns 400")
    void testHandlePropertyReference() {
        PropertyReferenceException ex = mock(PropertyReferenceException.class);
        when(ex.getPropertyName()).thenReturn("unknownField");

        ResponseEntity<ErrorResponse> response = exceptionHandler.handlePropertyReference(ex, request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getMessage()).contains("unknownField");
    }

    @Test
    @DisplayName("Handle general Exception returns 500")
    void testHandleGeneral() {
        Exception ex = new RuntimeException("Unexpected fatal failure");
        ResponseEntity<ErrorResponse> response = exceptionHandler.handleGeneral(ex, request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getStatus()).isEqualTo(500);
        assertThat(response.getBody().getError()).isEqualTo("Internal Server Error");
    }

    @Test
    @DisplayName("Test ErrorResponse getters and builder")
    void testErrorResponseProperties() {
        Instant now = Instant.now();
        ErrorResponse errorResponse = ErrorResponse.builder()
                .timestamp(now)
                .status(400)
                .error("Bad Request")
                .message("Test message")
                .path("/test")
                .validationErrors(Collections.singletonMap("field", "error"))
                .build();

        assertThat(errorResponse.getTimestamp()).isEqualTo(now);
        assertThat(errorResponse.getStatus()).isEqualTo(400);
        assertThat(errorResponse.getError()).isEqualTo("Bad Request");
        assertThat(errorResponse.getMessage()).isEqualTo("Test message");
        assertThat(errorResponse.getPath()).isEqualTo("/test");
        assertThat(errorResponse.getValidationErrors()).containsEntry("field", "error");
    }
}
