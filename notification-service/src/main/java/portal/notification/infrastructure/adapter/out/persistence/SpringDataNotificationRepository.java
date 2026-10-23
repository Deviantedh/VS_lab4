package portal.notification.infrastructure.adapter.out.persistence;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface SpringDataNotificationRepository extends JpaRepository<NotificationJpaEntity, Long> {

    @Query("""
        SELECT n FROM NotificationJpaEntity n
        WHERE (n.recipientUserId = :userId OR (n.recipientUserId IS NULL AND (n.targetRole IS NULL OR UPPER(n.targetRole) = UPPER(:role))))
          AND (:isRead IS NULL OR n.isRead = :isRead)
        ORDER BY n.createdAt DESC
    """)
    List<NotificationJpaEntity> findByRecipient(
            @Param("userId") Long userId,
            @Param("role") String role,
            @Param("isRead") Boolean isRead,
            Pageable pageable
    );

    @Query("""
        SELECT COUNT(n) FROM NotificationJpaEntity n
        WHERE (n.recipientUserId = :userId OR (n.recipientUserId IS NULL AND (n.targetRole IS NULL OR UPPER(n.targetRole) = UPPER(:role))))
          AND n.isRead = false
    """)
    long countUnread(
            @Param("userId") Long userId,
            @Param("role") String role
    );

    @Modifying
    @Query("""
        UPDATE NotificationJpaEntity n
        SET n.isRead = true, n.readAt = :readAt
        WHERE (n.recipientUserId = :userId OR (n.recipientUserId IS NULL AND (n.targetRole IS NULL OR UPPER(n.targetRole) = UPPER(:role))))
          AND n.isRead = false
    """)
    void markAllRead(
            @Param("userId") Long userId,
            @Param("role") String role,
            @Param("readAt") LocalDateTime readAt
    );
}
