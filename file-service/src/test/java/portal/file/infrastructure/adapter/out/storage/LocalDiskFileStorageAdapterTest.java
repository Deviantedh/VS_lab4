package portal.file.infrastructure.adapter.out.storage;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import portal.file.domain.exception.FileStorageException;

import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class LocalDiskFileStorageAdapterTest {

    @TempDir
    Path tempDir;

    @Test
    @DisplayName("LocalDiskFileStorageAdapter stores, reads, and deletes file bytes correctly")
    void testFileStorageOperations() {
        LocalDiskFileStorageAdapter storage = new LocalDiskFileStorageAdapter(tempDir.toString());

        byte[] content = "Пример контента для ЛР4".getBytes();
        String savedPath = storage.store("sample.txt", content);

        assertThat(savedPath).contains("sample.txt");

        byte[] readContent = storage.read(savedPath);
        assertThat(readContent).isEqualTo(content);

        storage.delete(savedPath);

        assertThatThrownBy(() -> storage.read(savedPath))
                .isInstanceOf(FileStorageException.class);
    }
}
