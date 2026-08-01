package com.lampify.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jws;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.util.Date;
import java.util.UUID;

@Component
public class JwtUtil {

    @Value("${app.jwt.secret}")
    private String jwtSecret;

    @Value("${app.jwt.access-expiration-ms}")
    private long jwtExpirationMs;

    @Value("${app.jwt.refresh-expiration-ms}")
    private long refreshExpirationMs;

    private Key getSigningKey() {
        return Keys.hmacShaKeyFor(jwtSecret.getBytes(StandardCharsets.UTF_8));
    }

    public String generateToken(String subject) {
        return generateAccessToken(subject, UUID.randomUUID().toString());
    }

    public String generateAccessToken(String subject, String jti) {
        Date now = new Date();
        Date expiration = new Date(now.getTime() + jwtExpirationMs);

        return Jwts.builder()
            .setSubject(subject)
            .setId(jti)
            .claim("type", "access")
            .setIssuedAt(now)
            .setExpiration(expiration)
            .signWith(getSigningKey(), SignatureAlgorithm.HS256)
            .compact();
    }

    public String generateRefreshToken(String subject, String jti) {
        Date now = new Date();
        Date expiration = new Date(now.getTime() + refreshExpirationMs);

        return Jwts.builder()
            .setSubject(subject)
            .setId(jti)
            .claim("type", "refresh")
            .setIssuedAt(now)
            .setExpiration(expiration)
            .signWith(getSigningKey(), SignatureAlgorithm.HS256)
            .compact();
    }

    public boolean validateToken(String token) {
        try {
            Jwts.parserBuilder()
                .setSigningKey(getSigningKey())
                .build()
                .parseClaimsJws(token);
            return true;
        } catch (Exception ex) {
            return false;
        }
    }

    public String getSubjectFromToken(String token) {
        Jws<Claims> claimsJws = Jwts.parserBuilder()
            .setSigningKey(getSigningKey())
            .build()
            .parseClaimsJws(token);
        return claimsJws.getBody().getSubject();
    }

    public String getJtiFromToken(String token) {
        Jws<Claims> claimsJws = Jwts.parserBuilder()
            .setSigningKey(getSigningKey())
            .build()
            .parseClaimsJws(token);
        return claimsJws.getBody().getId();
    }

    public String getTokenType(String token) {
        Jws<Claims> claimsJws = Jwts.parserBuilder()
            .setSigningKey(getSigningKey())
            .build()
            .parseClaimsJws(token);
        return claimsJws.getBody().get("type", String.class);
    }

    public Date getExpirationFromToken(String token) {
        Jws<Claims> claimsJws = Jwts.parserBuilder()
            .setSigningKey(getSigningKey())
            .build()
            .parseClaimsJws(token);
        return claimsJws.getBody().getExpiration();
    }
}
