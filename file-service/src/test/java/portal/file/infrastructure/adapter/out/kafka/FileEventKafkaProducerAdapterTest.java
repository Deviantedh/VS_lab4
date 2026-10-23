package portal.file.infrastructure.adapter.out.kafka;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;
import portal.dto.event.FileEventDto;
import portal.file.domain.model.StoredFile;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class FileEventKafkaProducerAdapterTest {

    @Mock
    private KafkaTemplate<String, Object> kafkaTemplate;

    private FileEventKafkaProducerAdapter adapter;

    @BeforeEach
    void setUp() {
        adapter = new FileEventKafkaProducerAdapter(kafkaTemplate);
    }

    @Test
    @DisplayName("Publish file uploaded event to file-events topic")
    void testPublishFileUploaded() {
        StoredFile file = StoredFile.builder()
                .id(101L)
                .filename("doc-uuid.pdf")
                .originalFilename("Report.pdf")
                .contentType("application/pdf")
                .size(4096L)
                .uploadedBy("testuser")
                .uploadedAt(LocalDateTime.now())
                .build();

        adapter.publishFileUploaded(file);

        ArgumentCaptor<FileEventDto> captor = ArgumentCaptor.forClass(FileEventDto.class);
        verify(kafkaTemplate).send(eq("file-events"), eq("101"), captor.capture());

        FileEventDto sent = captor.getValue();
        assertThat(sent.getEventType()).isEqualTo(FileEventDto.EventType.FILE_UPLOADED);
        assertThat(sent.getFileId()).isEqualTo(101L);
        assertThat(sent.getOriginalFilename()).isEqualTo("Report.pdf");
        assertThat(sent.getUploadedBy()).isEqualTo("testuser");
    }

    @Test
    @DisplayName("Publish file deleted event to file-events topic")
    void testPublishFileDeleted() {
        StoredFile file = StoredFile.builder()
                .id(102L)
                .filename("doc-uuid-2.pdf")
                .originalFilename("OldReport.pdf")
                .contentType("application/pdf")
                .size(1024L)
                .uploadedBy("testuser")
                .uploadedAt(LocalDateTime.now())
                .build();

        adapter.publishFileDeleted(file);

        ArgumentCaptor<FileEventDto> captor = ArgumentCaptor.forClass(FileEventDto.class);
        verify(kafkaTemplate).send(eq("file-events"), eq("102"), captor.capture());

        FileEventDto sent = captor.getValue();
        assertThat(sent.getEventType()).isEqualTo(FileEventDto.EventType.FILE_DELETED);
        assertThat(sent.getFileId()).isEqualTo(102L);
        assertThat(sent.getOriginalFilename()).isEqualTo("OldReport.pdf");
    }
}
