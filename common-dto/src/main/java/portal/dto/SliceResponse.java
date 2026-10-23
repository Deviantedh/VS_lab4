package portal.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.domain.Slice;

import java.util.Collections;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
public class SliceResponse<T> {
    private List<T> content;
    private int pageNumber;
    private int pageSize;
    private boolean hasNext;

    public SliceResponse(List<T> content, int pageNumber, int pageSize, boolean hasNext) {
        this.content = content != null ? content : Collections.emptyList();
        this.pageNumber = pageNumber;
        this.pageSize = pageSize;
        this.hasNext = hasNext;
    }

    public static <E> SliceResponse<E> of(List<E> content, int pageNumber, int pageSize, boolean hasNext) {
        return new SliceResponse<>(content, pageNumber, pageSize, hasNext);
    }

    public static <E> SliceResponse<E> of(List<E> content, boolean hasNext) {
        return new SliceResponse<>(content, 0, content != null ? content.size() : 0, hasNext);
    }

    public static <E> SliceResponse<E> empty() {
        return new SliceResponse<>(Collections.emptyList(), 0, 0, false);
    }

    public static <E> SliceResponse<E> from(Slice<E> slice) {
        if (slice == null) {
            return empty();
        }
        return new SliceResponse<>(
                slice.getContent(),
                slice.getNumber(),
                slice.getSize(),
                slice.hasNext()
        );
    }
}
