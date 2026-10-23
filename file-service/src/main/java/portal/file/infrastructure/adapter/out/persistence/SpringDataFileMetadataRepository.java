package portal.file.infrastructure.adapter.out.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

public interface SpringDataFileMetadataRepository extends JpaRepository<FileMetadataJpaEntity, Long> {
}
