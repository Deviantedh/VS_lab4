package portal.file.infrastructure.adapter.out.storage;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import portal.file.application.port.out.FileStoragePort;
import portal.file.domain.exception.FileStorageException;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.core.ResponseBytes;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3ClientBuilder;
import software.amazon.awssdk.services.s3.model.*;

import java.net.URI;

@Slf4j
@Component
@ConditionalOnProperty(
        name = "file.storage.type",
        havingValue = "s3",
        matchIfMissing = true
)
public class S3FileStorageAdapter implements FileStoragePort {

    private final S3Client s3Client;
    private final String bucketName;

    @Autowired
    public S3FileStorageAdapter(
            @Value("${file.storage.s3.endpoint:http://localhost:9000}") String endpoint,
            @Value("${file.storage.s3.access-key:minio_admin}") String accessKey,
            @Value("${file.storage.s3.secret-key:minio_secret}") String secretKey,
            @Value("${file.storage.s3.bucket:portal-files}") String bucketName,
            @Value("${file.storage.s3.region:us-east-1}") String region
    ) {
        this.bucketName = bucketName;
        S3ClientBuilder builder = S3Client.builder()
                .region(Region.of(region))
                .credentialsProvider(StaticCredentialsProvider.create(
                        AwsBasicCredentials.create(accessKey, secretKey)
                ));

        if (endpoint != null && !endpoint.isBlank()) {
            builder.endpointOverride(URI.create(endpoint))
                    .forcePathStyle(true);
        }

        this.s3Client = builder.build();
        initBucket();
    }

    public S3FileStorageAdapter(S3Client s3Client, String bucketName) {
        this.s3Client = s3Client;
        this.bucketName = bucketName;
    }

    private void initBucket() {
        try {
            s3Client.headBucket(HeadBucketRequest.builder().bucket(bucketName).build());
            log.info("Connected to existing S3 bucket: {}", bucketName);
        } catch (NoSuchBucketException e) {
            log.info("S3 bucket {} not found, creating...", bucketName);
            try {
                s3Client.createBucket(CreateBucketRequest.builder().bucket(bucketName).build());
                log.info("Successfully created S3 bucket: {}", bucketName);
            } catch (Exception ex) {
                log.warn("Could not create S3 bucket {}: {}", bucketName, ex.getMessage());
            }
        } catch (Exception e) {
            log.warn("S3 bucket check status for {}: {}", bucketName, e.getMessage());
        }
    }

    @Override
    public String store(String filename, byte[] data) {
        try {
            PutObjectRequest putRequest = PutObjectRequest.builder()
                    .bucket(bucketName)
                    .key(filename)
                    .build();

            s3Client.putObject(putRequest, RequestBody.fromBytes(data));
            log.info("Stored file to S3: bucket={}, key={}", bucketName, filename);
            return "s3://" + bucketName + "/" + filename;
        } catch (Exception e) {
            throw new FileStorageException("Ошибка сохранения файла в S3 (" + filename + "): " + e.getMessage(), e);
        }
    }

    @Override
    public byte[] read(String storagePath) {
        String key = extractKey(storagePath);
        try {
            GetObjectRequest getRequest = GetObjectRequest.builder()
                    .bucket(bucketName)
                    .key(key)
                    .build();

            ResponseBytes<GetObjectResponse> responseBytes = s3Client.getObjectAsBytes(getRequest);
            return responseBytes.asByteArray();
        } catch (NoSuchKeyException e) {
            throw new FileStorageException("Файл в S3 не найден: " + key, e);
        } catch (Exception e) {
            throw new FileStorageException("Ошибка чтения файла из S3 (" + key + "): " + e.getMessage(), e);
        }
    }

    @Override
    public void delete(String storagePath) {
        String key = extractKey(storagePath);
        try {
            DeleteObjectRequest deleteRequest = DeleteObjectRequest.builder()
                    .bucket(bucketName)
                    .key(key)
                    .build();

            s3Client.deleteObject(deleteRequest);
            log.info("Deleted file from S3: bucket={}, key={}", bucketName, key);
        } catch (Exception e) {
            log.warn("Не удалось удалить файл из S3 (key={}): {}", key, e.getMessage());
        }
    }

    private String extractKey(String storagePath) {
        if (storagePath == null) {
            return "";
        }
        String prefix = "s3://" + bucketName + "/";
        if (storagePath.startsWith(prefix)) {
            return storagePath.substring(prefix.length());
        }
        if (storagePath.startsWith("/")) {
            return storagePath.substring(1);
        }
        return storagePath;
    }
}
