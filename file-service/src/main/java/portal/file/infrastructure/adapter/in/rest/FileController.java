package portal.file.infrastructure.adapter.in.rest;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import portal.dto.FileMetadataDto;
import portal.file.application.port.in.DeleteFileUseCase;
import portal.file.application.port.in.DownloadFileUseCase;
import portal.file.application.port.in.GetFileMetadataUseCase;
import portal.file.application.port.in.UploadFileUseCase;
import portal.file.domain.exception.InvalidFileException;
import portal.file.domain.model.StoredFile;
import portal.file.infrastructure.security.UserPrincipal;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/files")
@RequiredArgsConstructor
@Tag(name = "Файловый сервис", description = "Загрузка, скачивание и управление метаданными файлов (Clean Architecture)")
public class FileController {

    private final UploadFileUseCase uploadFileUseCase;
    private final DownloadFileUseCase downloadFileUseCase;
    private final GetFileMetadataUseCase getFileMetadataUseCase;
    private final DeleteFileUseCase deleteFileUseCase;

    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Загрузить файл в хранилище (аватарки, документы, справки к отпуску)")
    public ResponseEntity<FileMetadataDto> uploadFile(
            @RequestParam("file") MultipartFile file,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        if (file.isEmpty()) {
            throw new InvalidFileException("Файл не должен быть пустым");
        }

        try {
            String uploadedBy = principal != null ? principal.getUsername() : "system";
            StoredFile stored = uploadFileUseCase.upload(
                    file.getOriginalFilename(),
                    file.getContentType(),
                    file.getBytes(),
                    uploadedBy
            );
            return ResponseEntity.ok(toDto(stored));
        } catch (IOException e) {
            throw new InvalidFileException("Ошибка чтения загруженного файла: " + e.getMessage());
        }
    }

    @GetMapping("/{id}/download")
    @Operation(summary = "Скачать файл (в виде вложения attachment)")
    public ResponseEntity<ByteArrayResource> downloadFile(@PathVariable Long id) {
        StoredFile file = downloadFileUseCase.getFile(id);
        byte[] data = downloadFileUseCase.downloadContent(id);

        String encodedName = URLEncoder.encode(file.getOriginalFilename(), StandardCharsets.UTF_8).replace("+", "%20");

        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(file.getContentType()))
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename*=UTF-8''" + encodedName)
                .contentLength(data.length)
                .body(new ByteArrayResource(data));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Просмотреть файл inline (для изображений и PDF)")
    public ResponseEntity<ByteArrayResource> viewFileInline(@PathVariable Long id) {
        StoredFile file = downloadFileUseCase.getFile(id);
        byte[] data = downloadFileUseCase.downloadContent(id);

        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(file.getContentType()))
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + file.getOriginalFilename() + "\"")
                .contentLength(data.length)
                .body(new ByteArrayResource(data));
    }

    @GetMapping("/{id}/metadata")
    @Operation(summary = "Получить метаданные файла")
    public ResponseEntity<FileMetadataDto> getMetadata(@PathVariable Long id) {
        StoredFile file = getFileMetadataUseCase.getMetadata(id);
        return ResponseEntity.ok(toDto(file));
    }

    @GetMapping
    @Operation(summary = "Список загруженных файлов с пагинацией")
    public ResponseEntity<List<FileMetadataDto>> listFiles(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        List<StoredFile> files = getFileMetadataUseCase.listFiles(page, size);
        return ResponseEntity.ok(files.stream().map(this::toDto).toList());
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Удалить файл")
    public ResponseEntity<Map<String, String>> deleteFile(
            @PathVariable Long id,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        String username = principal != null ? principal.getUsername() : "";
        String role = principal != null && principal.getRole() != null ? principal.getRole().name() : "";

        deleteFileUseCase.delete(id, username, role);
        return ResponseEntity.ok(Map.of("status", "DELETED", "fileId", String.valueOf(id)));
    }

    private FileMetadataDto toDto(StoredFile file) {
        if (file == null) return null;
        return FileMetadataDto.builder()
                .id(file.getId())
                .filename(file.getFilename())
                .originalFilename(file.getOriginalFilename())
                .contentType(file.getContentType())
                .size(file.getSize())
                .downloadUrl("/api/files/" + file.getId() + "/download")
                .uploadedBy(file.getUploadedBy())
                .uploadedAt(file.getUploadedAt())
                .build();
    }
}
