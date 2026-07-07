package com.mf.fertilizer.context;

import com.mf.fertilizer.constant.RoleEnum;

public final class UserContext {

    private static final ThreadLocal<CurrentUser> CURRENT_USER = new ThreadLocal<>();

    private UserContext() {
    }

    public static void set(Long userId, String username, String role, String userType) {
        set(new CurrentUser(userId, username, role, userType));
    }

    public static void set(CurrentUser currentUser) {
        CURRENT_USER.set(currentUser);
    }

    public static CurrentUser getCurrentUser() {
        return CURRENT_USER.get();
    }

    public static boolean isAuthenticated() {
        return getCurrentUser() != null;
    }

    public static Long getUserId() {
        var currentUser = getCurrentUser();
        return currentUser == null ? null : currentUser.userId();
    }

    public static Long requireUserId() {
        var userId = getUserId();
        if (userId == null) {
            throw new IllegalStateException("Current user is not authenticated");
        }
        return userId;
    }

    public static String getUsername() {
        var currentUser = getCurrentUser();
        return currentUser == null ? null : currentUser.username();
    }

    public static String getRole() {
        var currentUser = getCurrentUser();
        return currentUser == null ? null : currentUser.role();
    }

    public static String getUserType() {
        var currentUser = getCurrentUser();
        return currentUser == null ? null : currentUser.userType();
    }

    public static boolean isUserType(String userType) {
        return userType != null && userType.equals(getUserType());
    }

    public static boolean isClientUser() {
        return isUserType(RoleEnum.CONSUMER);
    }

    public static boolean isAdminUser() {
        return isUserType(RoleEnum.ADMIN);
    }

    public static boolean isMerchantUser() {
        return isUserType(RoleEnum.MERCHANT);
    }

    public static void clear() {
        CURRENT_USER.remove();
    }

    public record CurrentUser(Long userId, String username, String role, String userType) {
    }
}
