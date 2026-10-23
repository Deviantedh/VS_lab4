package portal.file.application.port.in;

import portal.file.domain.model.StoredFile;

import java.util.List;

public interface GetFileMetadataUseCase {
    StoredFile getMetadata(Long fileId);
    List<StoredFile> listFiles(int page, int size);
}
