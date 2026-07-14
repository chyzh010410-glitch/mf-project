package com.mf.datacenter.ai;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

@Service
public class HistoryAccessService {
    private final String internalToken;
    private final String identitySecret;
    public HistoryAccessService(@Value("${datacenter.internal-token:}") String internalToken,
                                @Value("${datacenter.internal-identity-secret:}") String identitySecret) {
        this.internalToken = internalToken; this.identitySecret = identitySecret;
    }
    public Identity verify(String token, String userId, String userType, String signature) {
        if (blank(token) || !MessageDigest.isEqual(token.getBytes(StandardCharsets.UTF_8), internalToken.getBytes(StandardCharsets.UTF_8))) throw new HistoryUnauthorizedException();
        if (blank(userId) || blank(userType) || blank(signature) || blank(identitySecret)) throw new HistoryForbiddenException("可信身份头缺失");
        if (!MessageDigest.isEqual(sign(userId + ":" + userType).getBytes(StandardCharsets.UTF_8), signature.getBytes(StandardCharsets.UTF_8))) throw new HistoryForbiddenException("可信身份校验失败");
        return new Identity(userId, userType);
    }
    private String sign(String content) { try { var mac = Mac.getInstance("HmacSHA256"); mac.init(new SecretKeySpec(identitySecret.getBytes(StandardCharsets.UTF_8), "HmacSHA256")); var bytes = mac.doFinal(content.getBytes(StandardCharsets.UTF_8)); var out = new StringBuilder(); for (byte b : bytes) out.append(String.format("%02x", b)); return out.toString(); } catch (Exception ex) { throw new IllegalStateException("identity signature unavailable", ex); } }
    private boolean blank(String value) { return value == null || value.isBlank(); }
    public record Identity(String userId, String userType) {}
    public static class HistoryUnauthorizedException extends RuntimeException {}
    public static class HistoryForbiddenException extends RuntimeException { public HistoryForbiddenException(String message) { super(message); } }
}
