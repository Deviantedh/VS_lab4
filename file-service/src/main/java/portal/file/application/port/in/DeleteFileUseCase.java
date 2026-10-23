package portal.file.application.port.in;

public interface DeleteFileUseCase {
    void delete(Long fileId, String username, String role);
}
