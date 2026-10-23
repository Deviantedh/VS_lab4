package portal.gateway;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.web.filter.CorsFilter;

import static org.assertj.core.api.Assertions.assertThat;

class GatewayCorsConfigTest {

    @Test
    @DisplayName("corsFilter bean is configured properly")
    void testCorsFilter() {
        GatewayCorsConfig config = new GatewayCorsConfig();
        CorsFilter filter = config.corsFilter();

        assertThat(filter).isNotNull();
    }
}
