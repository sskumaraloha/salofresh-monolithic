package com.salofresh.notification;

import com.salofresh.common.enums.NotificationChannel;
import com.salofresh.common.enums.NotificationType;
import com.salofresh.dto.notification.NotificationResponse;
import com.salofresh.entity.User;
import com.salofresh.response.PagedResponse;
import org.springframework.data.domain.Pageable;

public interface NotificationService {

    void createAndDispatch(User user, NotificationType type, NotificationChannel channel,
                            String title, String message, String referenceId, String referenceType);

    PagedResponse<NotificationResponse> getNotificationsForUser(Long userId, Pageable pageable);

    long getUnreadCount(Long userId);

    void markAsRead(Long userId, Long notificationId);

    void markAllAsRead(Long userId);
}
