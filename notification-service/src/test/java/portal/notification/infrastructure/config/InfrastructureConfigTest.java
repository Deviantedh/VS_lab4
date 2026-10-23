package portal.notification.infrastructure.config;

import io.swagger.v3.oas.models.OpenAPI;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.test.util.ReflectionTestUtils;
import portal.notification.NotificationServiceApplication;
import portal.notification.infrastructure.security.JwtAuthenticationFilter;
import portal.notification.infrastructure.security.SecurityConfig;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

class InfrastructureConfigTest {

    @Test
    @DisplayName("OpenApiConfig creates OpenAPI bean with SecurityScheme")
    void testOpenApiConfig() {
        OpenApiConfig config = new OpenApiConfig();
        OpenAPI openAPI = config.notificationOpenAPI();
        assertThat(openAPI).isNotNull();
        assertThat(openAPI.getInfo().getTitle()).contains("Notification");
        assertThat(openAPI.getComponents().getSecuritySchemes()).containsKey("Bearer Authentication");
    }

    @Test
    @DisplayName("KafkaConsumerConfig configures listener container factory")
    void testKafkaConsumerConfig() {
        KafkaConsumerConfig config = new KafkaConsumerConfig();
        ReflectionTestUtils.setField(config, "bootstrapServers", "localhost:9092");
        assertThat(config.consumerFactory()).isNotNull();
        ConcurrentKafkaListenerContainerFactory<String, Object> factory = config.kafkaListenerContainerFactory();
        assertThat(factory).isNotNull();
    }

    @Test
    @DisplayName("SecurityConfig constructor")
    void testSecurityConfig() {
        JwtAuthenticationFilter filter = mock(JwtAuthenticationFilter.class);
        SecurityConfig config = new SecurityConfig(filter);
        assertThat(config).isNotNull();
    }

    @Test
    @DisplayName("Application class can be instantiated")
    void testApplicationClass() {
        NotificationServiceApplication app = new NotificationServiceApplication();
        assertThat(app).isNotNull();
    }
}
