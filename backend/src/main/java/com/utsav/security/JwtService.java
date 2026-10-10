package com.utsav.security;

import com.utsav.common.Role;
import com.utsav.config.UtsavProperties;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import java.util.UUID;
import javax.crypto.SecretKey;
import org.springframework.stereotype.Service;

/** JWT issuance and parsing (access + refresh). */
@Service
public class JwtService {

  private final UtsavProperties props;
  private final SecretKey key;

  public JwtService(UtsavProperties props) {
    this.props = props;
    this.key = Keys.hmacShaKeyFor(props.getJwtSecret().getBytes(StandardCharsets.UTF_8));
  }

  public String issueAccessToken(UUID userId, String email, Role role) {
    Instant now = Instant.now();
    return Jwts.builder()
        .subject(userId.toString())
        .claim("email", email)
        .claim("role", role.name())
        .claim("typ", "access")
        .id(UUID.randomUUID().toString())
        .issuedAt(Date.from(now))
        .expiration(Date.from(now.plus(props.accessTtl())))
        .signWith(key)
        .compact();
  }

  public String issueRefreshToken(UUID userId) {
    Instant now = Instant.now();
    return Jwts.builder()
        .subject(userId.toString())
        .claim("typ", "refresh")
        .id(UUID.randomUUID().toString())
        .issuedAt(Date.from(now))
        .expiration(Date.from(now.plus(props.refreshTtl())))
        .signWith(key)
        .compact();
  }

  public AccessClaims parseAccessToken(String token) {
    var jws = parse(token);
    var claims = jws.getPayload();
    if (!"access".equals(claims.get("typ", String.class))) {
      throw new JwtException("not an access token");
    }
    return new AccessClaims(
        UUID.fromString(claims.getSubject()),
        claims.get("email", String.class),
        Role.valueOf(claims.get("role", String.class)));
  }

  public UUID parseRefreshTokenSubject(String token) {
    var claims = parse(token).getPayload();
    if (!"refresh".equals(claims.get("typ", String.class))) {
      throw new JwtException("not a refresh token");
    }
    return UUID.fromString(claims.getSubject());
  }

  private io.jsonwebtoken.Jws<io.jsonwebtoken.Claims> parse(String token) {
    return Jwts.parser().verifyWith(key).build().parseSignedClaims(token);
  }

  public record AccessClaims(UUID userId, String email, Role role) {}
}
