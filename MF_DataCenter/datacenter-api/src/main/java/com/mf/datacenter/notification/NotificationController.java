package com.mf.datacenter.notification;

import com.mf.datacenter.common.ApiResponse;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/notifications")
public class NotificationController {
    private final NotificationService service;
    public NotificationController(NotificationService service) { this.service = service; }
    @GetMapping public ApiResponse<List<NotificationService.Notification>> list() { return ApiResponse.ok(service.notifications()); }
    @GetMapping("/unread-count") public ApiResponse<Long> unreadCount() { return ApiResponse.ok(service.unreadCount()); }
    @PatchMapping("/{id}/read") public ApiResponse<Void> read(@PathVariable Long id) { service.markRead(id); return ApiResponse.ok(null); }
}
