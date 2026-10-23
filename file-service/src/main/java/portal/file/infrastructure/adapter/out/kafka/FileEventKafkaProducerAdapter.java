package portal.file.infrastructure.adapter.out.kafka;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;
import portal.dto.event.FileEventDto;
import portal.file.application.port.out.FileEventPort;
import portal.file.domain.model.StoredFile;

import java.time.Instant;

@Slf4j
@Component
@RequiredArgsConstructor
public class FileEventKafkaProducerAdapter implements FileEventPort {

    private final KafkaTemplate<String, Object> kafkaTemplate;

    @Override
    public void publishFileUploaded(StoredFile file) {
        FileEventDto event = FileEventDto.builder()
                .eventType(FileEventDto.EventType.FILE_UPLOADED)
                .fileId(file.getId())
                .filename(file.getFilename())
                .originalFilename(file.getOriginalFilename())
                .contentType(file.getContentType())
                .size(file.getSize())
                .uploadedBy(file.getUploadedBy())
                .timestamp(Instant.now())
                .build();

        log.info("Publishing file uploaded event: {}", event);
        kafkaTemplate.send("file-events", String.valueOf(file.getId()), event);
    }

    @Override
    public void publishFileDeleted(StoredFile file) {
        FileEventDto event = FileEventDto.builder()
                .eventType(FileEventDto.EventType.FILE_DELETED)
                .fileId(file.getId())
                .filename(file.getFilename())
                .originalFilename(file.getOriginalFilename())
                .contentType(file.getContentType())
                .size(file.getSize())
                .uploadedBy(file.getUploadedBy())
                .timestamp(Instant.now())
                .build();

        log.info("Publishing file deleted event: {}", event);
        kafkaTemplate.send("file-events", String.valueOf(file.getId()), event);
    }
}
