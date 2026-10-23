package portal.file.application.port.out;

import portal.file.domain.model.StoredFile;

public interface FileEventPort {
    void publishFileUploaded(StoredFile file);
    void publishFileDeleted(StoredFile file);
}
