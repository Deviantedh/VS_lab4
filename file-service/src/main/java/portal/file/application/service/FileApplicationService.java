package portal.file.application.service;

import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import portal.file.application.port.in.DeleteFileUseCase;
import portal.file.application.port.in.DownloadFileUseCase;
import portal.file.application.port.in.GetFileMetadataUseCase;
import portal.file.application.port.in.UploadFileUseCase;
import portal.file.application.port.out.FileEventPort;
import portal.file.application.port.out.FileMetadataRepositoryPort;
import portal.file.application.port.out.FileStoragePort;
import portal.file.domain.exception.FileNotFoundException;
import portal.file.domain.exception.InvalidFileException;
import portal.file.domain.model.StoredFile;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
public class FileApplicationService implements UploadFileUseCase, DownloadFileUseCase, GetFileMetadataUseCase, DeleteFileUseCase {

    private final FileStoragePort storagePort;
    private final FileMetadataRepositoryPort repositoryPort;
    private final FileEventPort eventPort;

    @Override
    public StoredFile upload(String originalFilename, String contentType, byte[] data, String uploadedBy) {
        if (data == null || data.length == 0) {
            throw new InvalidFileException("Файл не может быть пустым");
        }
        if (originalFilename == null || originalFilename.isBlank()) {
            originalFilename = "unnamed_file";
        }

        String extension = "";
        int dotIndex = originalFilename.lastIndexOf('.');
        if (dotIndex >= 0) {
            extension = originalFilename.substring(dotIndex);
        }

        String uniqueFilename = UUID.randomUUID() + extension;
        String storagePath = storagePort.store(uniqueFilename, data);

        StoredFile file = StoredFile.builder()
                .filename(uniqueFilename)
                .originalFilename(originalFilename)
                .contentType(contentType != null ? contentType : "application/octet-stream")
                .size((long) data.length)
                .storagePath(storagePath)
                .uploadedBy(uploadedBy != null ? uploadedBy : "anonymous")
                .uploadedAt(LocalDateTime.now())
                .build();

        StoredFile saved = repositoryPort.save(file);
        eventPort.publishFileUploaded(saved);
        return saved;
    }

    @Override
    @Transactional(readOnly = true)
    public byte[] downloadContent(Long fileId) {
        StoredFile file = repositoryPort.findById(fileId)
                .orElseThrow(() -> new FileNotFoundException(fileId));
        return storagePort.read(file.getStoragePath());
    }

    @Override
    @Transactional(readOnly = true)
    public StoredFile getFile(Long fileId) {
        return repositoryPort.findById(fileId)
                .orElseThrow(() -> new FileNotFoundException(fileId));
    }

    @Override
    @Transactional(readOnly = true)
    public StoredFile getMetadata(Long fileId) {
        return repositoryPort.findById(fileId)
                .orElseThrow(() -> new FileNotFoundException(fileId));
    }

    @Override
    @Transactional(readOnly = true)
    public List<StoredFile> listFiles(int page, int size) {
        if (size <= 0 || size > 50) {
            size = 20;
        }
        if (page < 0) {
            page = 0;
        }
        return repositoryPort.findAll(page, size);
    }

    @Override
    public void delete(Long fileId, String username, String role) {
        StoredFile file = repositoryPort.findById(fileId)
                .orElseThrow(() -> new FileNotFoundException(fileId));

        boolean canDelete = "ADMIN".equalsIgnoreCase(role)
                || (file.getUploadedBy() != null && file.getUploadedBy().equalsIgnoreCase(username));

        if (!canDelete) {
            throw new AccessDeniedException("Недостаточно прав для удаления файла");
        }

        storagePort.delete(file.getStoragePath());
        repositoryPort.deleteById(fileId);
        eventPort.publishFileDeleted(file);
    }
}
