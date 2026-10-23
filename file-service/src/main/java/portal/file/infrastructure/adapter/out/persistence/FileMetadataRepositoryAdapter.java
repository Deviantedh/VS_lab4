package portal.file.infrastructure.adapter.out.persistence;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;
import portal.file.application.port.out.FileMetadataRepositoryPort;
import portal.file.domain.model.StoredFile;

import java.util.List;
import java.util.Optional;

@Component
@RequiredArgsConstructor
public class FileMetadataRepositoryAdapter implements FileMetadataRepositoryPort {

    private final SpringDataFileMetadataRepository repository;

    @Override
    public StoredFile save(StoredFile file) {
        FileMetadataJpaEntity entity = toEntity(file);
        FileMetadataJpaEntity saved = repository.save(entity);
        return toDomain(saved);
    }

    @Override
    public Optional<StoredFile> findById(Long id) {
        return repository.findById(id).map(this::toDomain);
    }

    @Override
    public void deleteById(Long id) {
        repository.deleteById(id);
    }

    @Override
    public List<StoredFile> findAll(int page, int size) {
        return repository.findAll(PageRequest.of(page, size)).stream()
                .map(this::toDomain)
                .toList();
    }

    private StoredFile toDomain(FileMetadataJpaEntity entity) {
        if (entity == null) return null;
        return StoredFile.builder()
                .id(entity.getId())
                .filename(entity.getFilename())
                .originalFilename(entity.getOriginalFilename())
                .contentType(entity.getContentType())
                .size(entity.getSize())
                .storagePath(entity.getStoragePath())
                .uploadedBy(entity.getUploadedBy())
                .uploadedAt(entity.getUploadedAt())
                .build();
    }

    private FileMetadataJpaEntity toEntity(StoredFile domain) {
        if (domain == null) return null;
        return FileMetadataJpaEntity.builder()
                .id(domain.getId())
                .filename(domain.getFilename())
                .originalFilename(domain.getOriginalFilename())
                .contentType(domain.getContentType())
                .size(domain.getSize())
                .storagePath(domain.getStoragePath())
                .uploadedBy(domain.getUploadedBy())
                .uploadedAt(domain.getUploadedAt())
                .build();
    }
}
