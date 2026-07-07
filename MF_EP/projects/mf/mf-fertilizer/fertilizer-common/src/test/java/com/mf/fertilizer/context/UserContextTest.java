package com.mf.fertilizer.context;

import com.mf.fertilizer.constant.RoleEnum;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class UserContextTest {

    @AfterEach
    void clear() {
        UserContext.clear();
    }

    @Test
    void setPopulatesCurrentUser() {
        UserContext.set(1L, "admin", RoleEnum.ADMIN, RoleEnum.ADMIN);

        assertTrue(UserContext.isAuthenticated());
        assertEquals(1L, UserContext.requireUserId());
        assertEquals("admin", UserContext.getUsername());
        assertTrue(UserContext.isAdminUser());
        assertFalse(UserContext.isClientUser());
    }

    @Test
    void requireUserIdRejectsAnonymousAccess() {
        assertFalse(UserContext.isAuthenticated());
        assertThrows(IllegalStateException.class, UserContext::requireUserId);
    }
}
