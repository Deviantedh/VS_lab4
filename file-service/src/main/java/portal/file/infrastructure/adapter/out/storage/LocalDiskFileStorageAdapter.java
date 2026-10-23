package portal.file.infrastructure.adapter.out.storage;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import portal.file.application.port.out.FileStoragePort;
import portal.file.domain.exception.FileStorageException;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;

@Slf4j
@Component
public class LocalDiskFileStorageAdapter implements FileStoragePort {

    private final Path storageLocation;

    public LocalDiskFileStorageAdapter(@Value("${file.storage.path:/app/uploads}") String storagePath) {
        this.storageLocation = Paths.get(storagePath).toAbsolutePath().normalize();
        try {
            Files.createDirectories(this.storageLocation);
            log.info("Initialized local disk file storage at: {}", this.storageLocation);
        } catch (IOException e) {
            throw new FileStorageException("Не удалось создать директорию для хранения файлов: " + storagePath, e);
        }
    }

    @Override
    public String store(String filename, byte[] data) {
        try {
            Path targetPath = this.storageLocation.resolve(filename).normalize();
            Files.write(targetPath, data, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
            return targetPath.toString();
        } catch (IOException e) {
            throw new FileStorageException("Ошибка сохранения файла на диск: " + filename, e);
        }
    }

    @Override
    public byte[] read(String storagePath) {
        try {
            Path path = Paths.get(storagePath);
            if (!Files.exists(path)) {
                throw new FileStorageException("Файл на диске отсутствует: " + storagePath);
            }
            return Files.readAllBytes(path);
        } catch (IOException e) {
            throw new FileStorageException("Ошибка чтения файла: " + storagePath, e);
        }
    }

    @Override
    public void delete(String storagePath) {
        try {
            Path path = Paths.get(storagePath);
            Files.deleteIfExists(path);
        } catch (IOException e) {
            log.warn("Не удалось удалить файл с диска: {}", storagePath, e);
        }
    }
}
