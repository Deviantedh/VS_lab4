package portal.file.infrastructure.config;

import io.swagger.v3.oas.models.OpenAPI;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.test.util.ReflectionTestUtils;
import portal.file.FileServiceApplication;
import portal.file.domain.exception.FileNotFoundException;
import portal.file.domain.exception.FileStorageException;
import portal.file.infrastructure.security.JwtAuthenticationFilter;
import portal.file.infrastructure.security.SecurityConfig;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

class InfrastructureConfigTest {

    @Test
    @DisplayName("OpenApiConfig creates OpenAPI bean")
    void testOpenApiConfig() {
        OpenApiConfig config = new OpenApiConfig();
        OpenAPI openAPI = config.fileOpenAPI();
        assertThat(openAPI).isNotNull();
        assertThat(openAPI.getInfo().getTitle()).contains("File Service");
        assertThat(openAPI.getComponents().getSecuritySchemes()).containsKey("Bearer Authentication");
    }

    @Test
    @DisplayName("KafkaProducerConfig creates producer factory and KafkaTemplate")
    void testKafkaProducerConfig() {
        KafkaProducerConfig config = new KafkaProducerConfig();
        ReflectionTestUtils.setField(config, "bootstrapServers", "localhost:9092");
        assertThat(config.producerFactory()).isNotNull();
        KafkaTemplate<String, Object> template = config.kafkaTemplate();
        assertThat(template).isNotNull();
    }

    @Test
    @DisplayName("SecurityConfig instantiation")
    void testSecurityConfig() {
        JwtAuthenticationFilter filter = mock(JwtAuthenticationFilter.class);
        SecurityConfig config = new SecurityConfig(filter);
        assertThat(config).isNotNull();
    }

    @Test
    @DisplayName("Exception classes constructors coverage")
    void testExceptions() {
        FileNotFoundException e1 = new FileNotFoundException("msg");
        FileNotFoundException e2 = new FileNotFoundException(99L);
        assertThat(e1.getMessage()).isEqualTo("msg");
        assertThat(e2.getMessage()).contains("99");

        FileStorageException s1 = new FileStorageException("storage error");
        FileStorageException s2 = new FileStorageException("storage error", new RuntimeException());
        assertThat(s1.getMessage()).isEqualTo("storage error");
        assertThat(s2.getCause()).isNotNull();
    }

    @Test
    @DisplayName("Application class can be instantiated")
    void testApplicationClass() {
        FileServiceApplication app = new FileServiceApplication();
        assertThat(app).isNotNull();
    }
}
