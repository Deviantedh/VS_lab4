package portal.file.application.port.in;

import portal.file.domain.model.StoredFile;

public interface DownloadFileUseCase {
    byte[] downloadContent(Long fileId);
    StoredFile getFile(Long fileId);
}
