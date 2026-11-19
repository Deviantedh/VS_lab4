package portal.configserver;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.bus.BusBridge;
import org.springframework.cloud.bus.BusProperties;
import org.springframework.cloud.bus.event.RefreshRemoteApplicationEvent;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

@Service
public class ConfigRepoWatcherService {

    private static final Logger log = LoggerFactory.getLogger(ConfigRepoWatcherService.class);

    private final BusBridge busBridge;
    private final BusProperties busProperties;
    private final Map<String, Long> fileLastModifiedMap = new HashMap<>();
    private boolean initialized = false;

    @Value("${config.repo.watch-path:./config-repo}")
    private String watchPath;

    public ConfigRepoWatcherService(@Autowired(required = false) BusBridge busBridge,
                                    @Autowired(required = false) BusProperties busProperties) {
        this.busBridge = busBridge;
        this.busProperties = busProperties;
    }

    @Scheduled(fixedDelayString = "${config.repo.poll-interval-ms:5000}")
    public void checkForChanges() {
        if (busBridge == null) {
            return;
        }

        File repoDir = resolveRepoDir();
        if (repoDir == null || !repoDir.exists()) {
            return;
        }

        try (Stream<Path> paths = Files.walk(repoDir.toPath())) {
            List<Path> files = paths.filter(Files::isRegularFile)
                    .filter(p -> {
                        String name = p.getFileName().toString();
                        return name.endsWith(".yml") || name.endsWith(".yaml") || name.endsWith(".properties");
                    })
                    .toList();

            if (!initialized) {
                for (Path p : files) {
                    fileLastModifiedMap.put(p.toAbsolutePath().normalize().toString(), Files.getLastModifiedTime(p).toMillis());
                }
                initialized = true;
                log.info("ConfigRepoWatcherService initialized with {} config files in {}", files.size(), repoDir.getAbsolutePath());
                return;
            }

            boolean hasChanged = false;
            for (Path p : files) {
                String key = p.toAbsolutePath().normalize().toString();
                long currentMod = Files.getLastModifiedTime(p).toMillis();
                Long prevMod = fileLastModifiedMap.get(key);
                if (prevMod == null || prevMod != currentMod) {
                    fileLastModifiedMap.put(key, currentMod);
                    log.info("Detected change in config file: {}", p.getFileName());
                    hasChanged = true;
                }
            }

            if (hasChanged) {
                publishBusRefresh("**");
            }
        } catch (IOException e) {
            log.warn("Error while checking config repo for changes: {}", e.getMessage());
        }
    }

    public void publishBusRefresh(String destination) {
        if (busBridge == null) {
            log.warn("BusBridge is not configured; skipping Kafka bus refresh event");
            return;
        }

        String origin = (busProperties != null && busProperties.getId() != null)
                ? busProperties.getId()
                : "config-service:8888";

        log.info("Publishing RefreshRemoteApplicationEvent to Kafka bus destination '{}' from origin '{}'...", destination, origin);
        RefreshRemoteApplicationEvent event = new RefreshRemoteApplicationEvent(this, origin, destination);
        busBridge.send(event);
        log.info("RefreshRemoteApplicationEvent successfully published to Kafka bus!");
    }

    private File resolveRepoDir() {
        File f = new File(watchPath);
        if (f.exists()) {
            return f;
        }
        f = new File("../config-repo");
        if (f.exists()) {
            return f;
        }
        return null;
    }
}
