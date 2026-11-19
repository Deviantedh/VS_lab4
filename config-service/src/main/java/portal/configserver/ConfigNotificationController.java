package portal.configserver;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/config")
public class ConfigNotificationController {

    private final ConfigRepoWatcherService watcherService;

    public ConfigNotificationController(ConfigRepoWatcherService watcherService) {
        this.watcherService = watcherService;
    }

    @PostMapping("/bus-refresh")
    public ResponseEntity<Map<String, Object>> triggerBusRefresh(
            @RequestParam(defaultValue = "**") String destination) {
        watcherService.publishBusRefresh(destination);
        return ResponseEntity.ok(Map.of(
                "status", "BUS_REFRESH_TRIGGERED",
                "destination", destination,
                "channel", "Kafka"
        ));
    }
}
