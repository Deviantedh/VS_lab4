package portal.file.application.port.out;

import portal.file.domain.model.StoredFile;

import java.util.List;
import java.util.Optional;

public interface FileMetadataRepositoryPort {
    StoredFile save(StoredFile file);
    Optional<StoredFile> findById(Long id);
    void deleteById(Long id);
    List<StoredFile> findAll(int page, int size);
}
