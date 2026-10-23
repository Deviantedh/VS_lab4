package portal.notification.domain.exception;

public class NotificationNotFoundException extends RuntimeException {
    public NotificationNotFoundException(Long id) {
        super("Уведомление с ID " + id + " не найдено");
    }
}
