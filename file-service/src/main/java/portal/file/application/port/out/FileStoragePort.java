package portal.file.application.port.out;

public interface FileStoragePort {
    String store(String filename, byte[] data);
    byte[] read(String storagePath);
    void delete(String storagePath);
}
