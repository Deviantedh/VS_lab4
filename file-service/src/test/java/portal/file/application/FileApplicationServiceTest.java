package portal.file.application;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;
import portal.file.application.port.out.FileEventPort;
import portal.file.application.port.out.FileMetadataRepositoryPort;
import portal.file.application.port.out.FileStoragePort;
import portal.file.application.service.FileApplicationService;
import portal.file.domain.exception.FileNotFoundException;
import portal.file.domain.exception.InvalidFileException;
import portal.file.domain.model.StoredFile;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class FileApplicationServiceTest {

    @Mock
    private FileStoragePort storagePort;

    @Mock
    private FileMetadataRepositoryPort repositoryPort;

    @Mock
    private FileEventPort eventPort;

    private FileApplicationService service;

    @BeforeEach
    void setUp() {
        service = new FileApplicationService(storagePort, repositoryPort, eventPort);
    }

    @Test
    @DisplayName("upload saves file to storage, saves metadata and publishes event")
    void testUploadSuccess() {
        byte[] data = "Hello World".getBytes();
        when(storagePort.store(anyString(), eq(data))).thenReturn("/storage/unique.png");
        when(repositoryPort.save(any(StoredFile.class))).thenAnswer(inv -> {
            StoredFile f = inv.getArgument(0);
            return f.toBuilder().id(10L).build();
        });

        StoredFile result = service.upload("avatar.png", "image/png", data, "admin");

        assertThat(result.getId()).isEqualTo(10L);
        assertThat(result.getOriginalFilename()).isEqualTo("avatar.png");
        assertThat(result.getSize()).isEqualTo((long) data.length);
        assertThat(result.getUploadedBy()).isEqualTo("admin");

        verify(storagePort).store(anyString(), eq(data));
        verify(repositoryPort).save(any(StoredFile.class));
        verify(eventPort).publishFileUploaded(any(StoredFile.class));
    }

    @Test
    @DisplayName("upload throws InvalidFileException for empty byte array")
    void testUploadEmptyFile() {
        assertThatThrownBy(() -> service.upload("empty.txt", "text/plain", new byte[0], "user"))
                .isInstanceOf(InvalidFileException.class)
                .hasMessageContaining("не может быть пустым");
    }

    @Test
    @DisplayName("downloadContent reads bytes from storage port")
    void testDownloadContent() {
        StoredFile file = StoredFile.builder()
                .id(1L)
                .storagePath("/storage/test.txt")
                .build();

        byte[] expected = "test content".getBytes();
        when(repositoryPort.findById(1L)).thenReturn(Optional.of(file));
        when(storagePort.read("/storage/test.txt")).thenReturn(expected);

        byte[] actual = service.downloadContent(1L);

        assertThat(actual).isEqualTo(expected);
    }

    @Test
    @DisplayName("downloadContent throws FileNotFoundException when id missing")
    void testDownloadContentNotFound() {
        when(repositoryPort.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.downloadContent(999L))
                .isInstanceOf(FileNotFoundException.class)
                .hasMessageContaining("999");
    }

    @Test
    @DisplayName("delete succeeds for owner and publishes event")
    void testDeleteOwner() {
        StoredFile file = StoredFile.builder()
                .id(5L)
                .storagePath("/storage/file.pdf")
                .uploadedBy("ivanov")
                .build();

        when(repositoryPort.findById(5L)).thenReturn(Optional.of(file));

        service.delete(5L, "ivanov", "EMPLOYEE");

        verify(storagePort).delete("/storage/file.pdf");
        verify(repositoryPort).deleteById(5L);
        verify(eventPort).publishFileDeleted(file);
    }

    @Test
    @DisplayName("delete succeeds for ADMIN even if not owner")
    void testDeleteAdmin() {
        StoredFile file = StoredFile.builder()
                .id(5L)
                .storagePath("/storage/file.pdf")
                .uploadedBy("ivanov")
                .build();

        when(repositoryPort.findById(5L)).thenReturn(Optional.of(file));

        service.delete(5L, "admin_user", "ADMIN");

        verify(storagePort).delete("/storage/file.pdf");
        verify(repositoryPort).deleteById(5L);
        verify(eventPort).publishFileDeleted(file);
    }

    @Test
    @DisplayName("delete throws AccessDeniedException for non-owner non-admin")
    void testDeleteAccessDenied() {
        StoredFile file = StoredFile.builder()
                .id(5L)
                .storagePath("/storage/file.pdf")
                .uploadedBy("ivanov")
                .build();

        when(repositoryPort.findById(5L)).thenReturn(Optional.of(file));

        assertThatThrownBy(() -> service.delete(5L, "petrov", "EMPLOYEE"))
                .isInstanceOf(AccessDeniedException.class)
                .hasMessageContaining("Недостаточно прав");
    }
}
