package portal.file.domain;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import portal.file.domain.model.StoredFile;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

class FileDomainTest {

    @Test
    @DisplayName("StoredFile isImage and isPdf detect content types accurately")
    void testContentTypeChecks() {
        StoredFile image = StoredFile.builder()
                .id(1L)
                .filename("test.png")
                .contentType("image/png")
                .build();

        assertThat(image.isImage()).isTrue();
        assertThat(image.isPdf()).isFalse();

        StoredFile pdf = StoredFile.builder()
                .id(2L)
                .filename("document.pdf")
                .contentType("application/pdf")
                .build();

        assertThat(pdf.isImage()).isFalse();
        assertThat(pdf.isPdf()).isTrue();

        StoredFile text = StoredFile.builder()
                .id(3L)
                .filename("notes.txt")
                .contentType("text/plain")
                .build();

        assertThat(text.isImage()).isFalse();
        assertThat(text.isPdf()).isFalse();
    }
}
