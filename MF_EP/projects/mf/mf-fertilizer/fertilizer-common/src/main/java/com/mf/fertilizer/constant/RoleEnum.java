package com.mf.fertilizer.constant;

public interface RoleEnum {

    String PUBLIC = "public";
    String CLIENT = "client";
    String ADMIN = "admin";
    String MERCHANT = "merchant";
    String SUPER_ADMIN = "super_admin";
    String OPERATOR = "operator";
    String CONSUMER = "consumer";

    static boolean hasRole(String requiredRole, String currentRole, String userType) {
        if (PUBLIC.equals(requiredRole)) {
            return true;
        }
        if (CLIENT.equals(requiredRole)) {
            return CONSUMER.equals(userType);
        }
        if (ADMIN.equals(requiredRole)) {
            return ADMIN.equals(userType) && (ADMIN.equals(currentRole) || OPERATOR.equals(currentRole));
        }
        if (MERCHANT.equals(requiredRole)) {
            return MERCHANT.equals(userType);
        }
        if (SUPER_ADMIN.equals(requiredRole)) {
            return ADMIN.equals(userType) && ADMIN.equals(currentRole);
        }
        return requiredRole != null && requiredRole.equals(currentRole);
    }
}
