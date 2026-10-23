package portal.notification.domain.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Getter
@Builder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
public class Notification {

    private Long id;
    private Long recipientUserId;
    private Long recipientEmployeeId;
    private String targetRole;
    private String title;
    private String message;
    private NotificationType type;
    private String link;
    private boolean isRead;
    private LocalDateTime createdAt;
    private LocalDateTime readAt;

    public void markAsRead() {
        this.isRead = true;
        this.readAt = LocalDateTime.now();
    }

    public boolean belongsToUser(Long userId) {
        return this.recipientUserId != null && this.recipientUserId.equals(userId);
    }
}
