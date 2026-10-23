package portal.notification.application.port.in;

import portal.notification.domain.model.Notification;

public interface CreateNotificationUseCase {
    Notification create(Notification notification);
}
