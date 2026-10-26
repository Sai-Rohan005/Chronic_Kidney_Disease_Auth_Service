package services;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import java.util.Optional;
import java.util.UUID;

@Service
public class JwtTokenService {

    private static final Duration TOKEN_TTL = Duration.ofMinutes(60);
    private static final String TOKEN_KEY_PREFIX = "auth:token:";

    private final SecretKey signingKey;
    private final StringRedisTemplate redisTemplate;

    public JwtTokenService(
            @Value("${app.jwt.secret}") String base64Secret,
            StringRedisTemplate redisTemplate
    ) {
        byte[] keyBytes = Decoders.BASE64.decode(base64Secret);
        this.signingKey = Keys.hmacShaKeyFor(keyBytes);
        this.redisTemplate = redisTemplate;
    }

    public IssuedToken issue(String email, String role) {
        Instant now = Instant.now();
        Instant expiresAt = now.plus(TOKEN_TTL);
        String tokenId = UUID.randomUUID().toString();

        String token = Jwts.builder()
                .subject(email)
                .claim("role", role)
                .id(tokenId)
                .issuedAt(Date.from(now))
                .expiration(Date.from(expiresAt))
                .signWith(signingKey)
                .compact();

        redisTemplate.opsForValue().set(
                redisKey(tokenId),
                token,
                TOKEN_TTL
        );
        return new IssuedToken(token, expiresAt);
    }

    public Optional<AuthenticatedUser> authenticate(String token) {
        try {
            Claims claims = Jwts.parser()
                    .verifyWith(signingKey)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
            String tokenId = claims.getId();
            String email = claims.getSubject();
            String role = claims.get("role", String.class);

            if (tokenId == null || email == null || role == null) {
                return Optional.empty();
            }

            String storedToken = redisTemplate.opsForValue().get(redisKey(tokenId));
            if (storedToken == null || !MessageDigest.isEqual(
                    storedToken.getBytes(StandardCharsets.UTF_8),
                    token.getBytes(StandardCharsets.UTF_8))) {
                return Optional.empty();
            }
            return Optional.of(new AuthenticatedUser(email, role));
        } catch (JwtException | IllegalArgumentException exception) {
            return Optional.empty();
        }
    }

    public boolean revoke(String token) {
        try {
            Claims claims = Jwts.parser()
                    .verifyWith(signingKey)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
            String tokenId = claims.getId();
            if (tokenId == null) {
                return false;
            }
            return Boolean.TRUE.equals(redisTemplate.delete(redisKey(tokenId)));
        } catch (JwtException | IllegalArgumentException exception) {
            return false;
        }
    }

    public static String bearerToken(String authorizationHeader) {
        if (authorizationHeader == null
                || !authorizationHeader.regionMatches(true, 0, "Bearer ", 0, 7)) {
            return null;
        }
        String token = authorizationHeader.substring(7).trim();
        return token.isEmpty() ? null : token;
    }

    private static String redisKey(String tokenId) {
        return TOKEN_KEY_PREFIX + tokenId;
    }

    public record IssuedToken(String value, Instant expiresAt) {}

    public record AuthenticatedUser(String email, String role) {}
}
