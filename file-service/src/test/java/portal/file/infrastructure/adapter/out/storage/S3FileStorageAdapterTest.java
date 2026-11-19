package portal.file.infrastructure.adapter.out.storage;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import portal.file.domain.exception.FileStorageException;
import software.amazon.awssdk.core.ResponseBytes;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.*;

import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class S3FileStorageAdapterTest {

    @Mock
    private S3Client s3Client;

    private S3FileStorageAdapter adapter;
    private static final String BUCKET = "test-portal-files";

    @BeforeEach
    void setUp() {
        adapter = new S3FileStorageAdapter(s3Client, BUCKET);
    }

    @Test
    @DisplayName("store() successfully saves bytes to S3 and returns S3 URI")
    void testStoreSuccess() {
        byte[] data = "Hello S3 Storage".getBytes(StandardCharsets.UTF_8);
        when(s3Client.putObject(any(PutObjectRequest.class), any(RequestBody.class)))
                .thenReturn(PutObjectResponse.builder().build());

        String uri = adapter.store("doc-123.pdf", data);

        assertThat(uri).isEqualTo("s3://test-portal-files/doc-123.pdf");
        verify(s3Client, times(1)).putObject(any(PutObjectRequest.class), any(RequestBody.class));
    }

    @Test
    @DisplayName("store() throws FileStorageException on S3 failure")
    void testStoreFailure() {
        byte[] data = "Failure data".getBytes(StandardCharsets.UTF_8);
        when(s3Client.putObject(any(PutObjectRequest.class), any(RequestBody.class)))
                .thenThrow(S3Exception.builder().message("Access Denied").build());

        assertThatThrownBy(() -> adapter.store("error.pdf", data))
                .isInstanceOf(FileStorageException.class)
                .hasMessageContaining("Ошибка сохранения файла в S3");
    }

    @Test
    @DisplayName("read() retrieves object bytes from S3")
    void testReadSuccess() {
        byte[] expected = "File content in S3".getBytes(StandardCharsets.UTF_8);
        GetObjectResponse response = GetObjectResponse.builder().contentLength((long) expected.length).build();
        ResponseBytes<GetObjectResponse> responseBytes = ResponseBytes.fromByteArray(response, expected);

        when(s3Client.getObjectAsBytes(any(GetObjectRequest.class))).thenReturn(responseBytes);

        byte[] actual = adapter.read("s3://test-portal-files/doc-123.pdf");

        assertThat(actual).isEqualTo(expected);
        verify(s3Client, times(1)).getObjectAsBytes(any(GetObjectRequest.class));
    }

    @Test
    @DisplayName("read() throws FileStorageException when key not found")
    void testReadNotFound() {
        when(s3Client.getObjectAsBytes(any(GetObjectRequest.class)))
                .thenThrow(NoSuchKeyException.builder().message("Key not found").build());

        assertThatThrownBy(() -> adapter.read("s3://test-portal-files/missing.pdf"))
                .isInstanceOf(FileStorageException.class)
                .hasMessageContaining("Файл в S3 не найден");
    }

    @Test
    @DisplayName("delete() calls deleteObject on S3 client")
    void testDeleteSuccess() {
        when(s3Client.deleteObject(any(DeleteObjectRequest.class)))
                .thenReturn(DeleteObjectResponse.builder().build());

        adapter.delete("s3://test-portal-files/to-delete.pdf");

        verify(s3Client, times(1)).deleteObject(any(DeleteObjectRequest.class));
    }
}
