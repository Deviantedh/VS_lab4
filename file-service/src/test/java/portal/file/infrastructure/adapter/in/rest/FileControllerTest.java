package portal.file.infrastructure.adapter.in.rest;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockMultipartFile;
import portal.dto.FileMetadataDto;
import portal.dto.RoleCode;
import portal.file.application.port.in.DeleteFileUseCase;
import portal.file.application.port.in.DownloadFileUseCase;
import portal.file.application.port.in.GetFileMetadataUseCase;
import portal.file.application.port.in.UploadFileUseCase;
import portal.file.domain.exception.InvalidFileException;
import portal.file.domain.model.StoredFile;
import portal.file.infrastructure.security.UserPrincipal;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FileControllerTest {

    @Mock
    private UploadFileUseCase uploadFileUseCase;

    @Mock
    private DownloadFileUseCase downloadFileUseCase;

    @Mock
    private GetFileMetadataUseCase getFileMetadataUseCase;

    @Mock
    private DeleteFileUseCase deleteFileUseCase;

    @InjectMocks
    private FileController controller;

    @Test
    @DisplayName("uploadFile successfully processes multipart file and returns metadata DTO")
    void testUploadFile() {
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "avatar.png",
                "image/png",
                "dummy content".getBytes()
        );
        UserPrincipal principal = UserPrincipal.fromClaims(1L, "admin", RoleCode.ADMIN, null);

        StoredFile stored = StoredFile.builder()
                .id(1L)
                .filename("uuid-avatar.png")
                .originalFilename("avatar.png")
                .contentType("image/png")
                .size(13L)
                .storagePath("/tmp/uuid-avatar.png")
                .uploadedBy("admin")
                .uploadedAt(LocalDateTime.now())
                .build();

        when(uploadFileUseCase.upload(eq("avatar.png"), eq("image/png"), any(), eq("admin")))
                .thenReturn(stored);

        ResponseEntity<FileMetadataDto> response = controller.uploadFile(file, principal);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getId()).isEqualTo(1L);
        assertThat(response.getBody().getOriginalFilename()).isEqualTo("avatar.png");
    }

    @Test
    @DisplayName("uploadFile throws InvalidFileException when file is empty")
    void testUploadEmptyFile() {
        MockMultipartFile emptyFile = new MockMultipartFile("file", "empty.txt", "text/plain", new byte[0]);
        UserPrincipal principal = UserPrincipal.fromClaims(1L, "admin", RoleCode.ADMIN, null);

        assertThatThrownBy(() -> controller.uploadFile(emptyFile, principal))
                .isInstanceOf(InvalidFileException.class)
                .hasMessageContaining("не должен быть пустым");
    }

    @Test
    @DisplayName("downloadFile returns ByteArrayResource with attachment header")
    void testDownloadFile() {
        StoredFile file = StoredFile.builder()
                .id(5L)
                .originalFilename("отчет.pdf")
                .contentType("application/pdf")
                .build();

        byte[] content = "PDF-BYTES".getBytes();
        when(downloadFileUseCase.getFile(5L)).thenReturn(file);
        when(downloadFileUseCase.downloadContent(5L)).thenReturn(content);

        ResponseEntity<ByteArrayResource> response = controller.downloadFile(5L);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getHeaders().getContentDisposition().toString()).contains("attachment;");
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getByteArray()).isEqualTo(content);
    }

    @Test
    @DisplayName("viewFileInline returns inline header")
    void testViewFileInline() {
        StoredFile file = StoredFile.builder()
                .id(10L)
                .originalFilename("photo.jpg")
                .contentType("image/jpeg")
                .build();

        byte[] content = "JPG-BYTES".getBytes();
        when(downloadFileUseCase.getFile(10L)).thenReturn(file);
        when(downloadFileUseCase.downloadContent(10L)).thenReturn(content);

        ResponseEntity<ByteArrayResource> response = controller.viewFileInline(10L);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getHeaders().getContentDisposition().toString()).contains("inline;");
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getByteArray()).isEqualTo(content);
    }

    @Test
    @DisplayName("getMetadata returns file metadata DTO")
    void testGetMetadata() {
        StoredFile file = StoredFile.builder()
                .id(20L)
                .originalFilename("test.pdf")
                .contentType("application/pdf")
                .size(1024L)
                .uploadedBy("ivanov")
                .build();

        when(getFileMetadataUseCase.getMetadata(20L)).thenReturn(file);

        ResponseEntity<FileMetadataDto> response = controller.getMetadata(20L);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getId()).isEqualTo(20L);
    }

    @Test
    @DisplayName("listFiles returns paged list of metadata DTOs")
    void testListFiles() {
        StoredFile file = StoredFile.builder().id(1L).originalFilename("f.png").contentType("image/png").build();
        when(getFileMetadataUseCase.listFiles(0, 20)).thenReturn(List.of(file));

        ResponseEntity<List<FileMetadataDto>> response = controller.listFiles(0, 20);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody()).hasSize(1);
    }

    @Test
    @DisplayName("deleteFile invokes delete use case and returns DELETED status")
    void testDeleteFile() {
        UserPrincipal principal = UserPrincipal.fromClaims(1L, "admin", RoleCode.ADMIN, null);

        ResponseEntity<Map<String, String>> response = controller.deleteFile(42L, principal);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().get("status")).isEqualTo("DELETED");
        verify(deleteFileUseCase).delete(42L, "admin", "ADMIN");
    }
}
