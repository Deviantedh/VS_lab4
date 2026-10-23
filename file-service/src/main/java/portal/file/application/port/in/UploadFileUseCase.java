package portal.file.application.port.in;

import portal.file.domain.model.StoredFile;

public interface UploadFileUseCase {
    StoredFile upload(String originalFilename, String contentType, byte[] data, String uploadedBy);
}
