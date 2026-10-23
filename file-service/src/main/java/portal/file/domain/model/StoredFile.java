package portal.file.domain.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Getter
@Builder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
public class StoredFile {

    private Long id;
    private String filename;
    private String originalFilename;
    private String contentType;
    private Long size;
    private String storagePath;
    private String uploadedBy;
    private LocalDateTime uploadedAt;

    public boolean isImage() {
        return contentType != null && contentType.startsWith("image/");
    }

    public boolean isPdf() {
        return "application/pdf".equalsIgnoreCase(contentType);
    }
}
