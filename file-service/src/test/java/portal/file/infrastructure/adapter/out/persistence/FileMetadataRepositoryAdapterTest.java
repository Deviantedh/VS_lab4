package portal.file.infrastructure.adapter.out.persistence;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import portal.file.domain.model.StoredFile;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FileMetadataRepositoryAdapterTest {

    @Mock
    private SpringDataFileMetadataRepository repository;

    private FileMetadataRepositoryAdapter adapter;

    @BeforeEach
    void setUp() {
        adapter = new FileMetadataRepositoryAdapter(repository);
    }

    @Test
    @DisplayName("save maps domain to entity, saves, and maps back to domain")
    void testSave() {
        StoredFile domain = StoredFile.builder()
                .filename("uuid-test.pdf")
                .originalFilename("test.pdf")
                .contentType("application/pdf")
                .size(100L)
                .storagePath("/tmp/uuid-test.pdf")
                .uploadedBy("ivanov")
                .uploadedAt(LocalDateTime.now())
                .build();

        FileMetadataJpaEntity entity = FileMetadataJpaEntity.builder()
                .id(1L)
                .filename("uuid-test.pdf")
                .originalFilename("test.pdf")
                .contentType("application/pdf")
                .size(100L)
                .storagePath("/tmp/uuid-test.pdf")
                .uploadedBy("ivanov")
                .uploadedAt(domain.getUploadedAt())
                .build();

        when(repository.save(any(FileMetadataJpaEntity.class))).thenReturn(entity);

        StoredFile saved = adapter.save(domain);

        assertThat(saved.getId()).isEqualTo(1L);
        assertThat(saved.getOriginalFilename()).isEqualTo("test.pdf");
    }

    @Test
    @DisplayName("findById retrieves and maps entity to domain")
    void testFindById() {
        FileMetadataJpaEntity entity = FileMetadataJpaEntity.builder()
                .id(10L)
                .filename("img.png")
                .originalFilename("avatar.png")
                .contentType("image/png")
                .size(500L)
                .storagePath("/tmp/img.png")
                .uploadedBy("admin")
                .uploadedAt(LocalDateTime.now())
                .build();

        when(repository.findById(10L)).thenReturn(Optional.of(entity));

        Optional<StoredFile> result = adapter.findById(10L);

        assertThat(result).isPresent();
        assertThat(result.get().getId()).isEqualTo(10L);
        assertThat(result.get().getOriginalFilename()).isEqualTo("avatar.png");
    }

    @Test
    @DisplayName("findAll pages and maps entities to domain")
    void testFindAll() {
        FileMetadataJpaEntity entity = FileMetadataJpaEntity.builder()
                .id(10L)
                .filename("img.png")
                .originalFilename("avatar.png")
                .contentType("image/png")
                .size(500L)
                .storagePath("/tmp/img.png")
                .uploadedBy("admin")
                .uploadedAt(LocalDateTime.now())
                .build();

        when(repository.findAll(any(Pageable.class))).thenReturn(new PageImpl<>(List.of(entity)));

        List<StoredFile> list = adapter.findAll(0, 10);

        assertThat(list).hasSize(1);
        assertThat(list.get(0).getId()).isEqualTo(10L);
    }

    @Test
    @DisplayName("deleteById delegates to repository")
    void testDeleteById() {
        adapter.deleteById(15L);
        verify(repository).deleteById(15L);
    }
}
