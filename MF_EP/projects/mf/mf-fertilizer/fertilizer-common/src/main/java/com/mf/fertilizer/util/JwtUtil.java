package com.mf.fertilizer.util;

import com.mf.fertilizer.constant.RoleEnum;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

public final class JwtUtil {

    private static final String SECRET = "miaoFeiFertilizerJwtSecretKey2026!@#%";
    private static final long EXPIRE_MS = 7 * 24 * 60 * 60 * 1000L;
    private static final SecretKey KEY = Keys.hmacShaKeyFor(SECRET.getBytes(StandardCharsets.UTF_8));

    private JwtUtil() {}

    public static String generate(Long userId, String username, String role) {
        return generateWithUserType(userId, username, role, RoleEnum.ADMIN);
    }

    public static String generateWithUserType(Long userId, String username, String role, String userType) {
        Date now = new Date();
        return Jwts.builder()
                .id(String.valueOf(userId))
                .subject(username)
                .claim("role", role)
                .claim("userType", userType)
                .issuedAt(now)
                .expiration(new Date(now.getTime() + EXPIRE_MS))
                .signWith(KEY)
                .compact();
    }

    public static Claims parse(String token) {
        return Jwts.parser()
                .verifyWith(KEY)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    public static Long getUserId(Claims claims) {
        return Long.valueOf(claims.getId());
    }

    public static String getUsername(Claims claims) {
        return claims.getSubject();
    }

    public static String getRole(Claims claims) {
        return claims.get("role", String.class);
    }

    public static String getUserType(Claims claims) {
        return claims.get("userType", String.class);
    }
}
