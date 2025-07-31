package com.minidocto.shared.util;

import java.util.Date;
import java.util.Map;
import io.jsonwebtoken.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.minidocto.shared.config.JwtProperties;

@Component
public class JwtUtil {

    private final JwtProperties jwtProperties;

    private static final Logger logger = LoggerFactory.getLogger(JwtUtil.class);

    @Autowired
    public JwtUtil(JwtProperties jwtProperties) {
        this.jwtProperties = jwtProperties;
    }

    public String generateToken(String username, Map<String, Object> claims) {
        Date now = new Date();
        logger.info("Generating access token for username: {}", username);
        return Jwts.builder()
            .setClaims(claims)
            .setSubject(username)
            .setIssuedAt(now)
            .setExpiration(new Date(now.getTime() + jwtProperties.getExpirationMs()))
            .signWith(SignatureAlgorithm.HS256, jwtProperties.getSecret())
            .compact();
    }

    public String generateRefreshToken(String username, Map<String, Object> claims) {
        Date now = new Date();
        logger.info("Generating refresh token for username: {}", username);
        return Jwts.builder()
            .setClaims(claims)
            .setSubject(username)
            .setIssuedAt(now)
            .setExpiration(new Date(now.getTime() + jwtProperties.getRefreshExpirationMs()))
            .signWith(SignatureAlgorithm.HS256, jwtProperties.getSecret())
            .compact();
    }

    public boolean validateToken(String token) {
        try {
            Claims claims = Jwts.parser().setSigningKey(jwtProperties.getSecret()).parseClaimsJws(token).getBody();
            Date expiration = claims.getExpiration();
            if (expiration.before(new Date())) {
                logger.warn("Token has expired");
                return false;
            }
            logger.info("Token is valid");
            return true;
        } catch (JwtException e) {
            logger.error("Token validation failed: {}", e.getMessage());
            return false;
        }
    }

    public String extractUsername(String token) {
        try {
            Claims claims = Jwts.parser().setSigningKey(jwtProperties.getSecret()).parseClaimsJws(token).getBody();
            return claims.getSubject();
        } catch (JwtException e) {
            logger.error("Failed to extract username from token: {}", e.getMessage());
            return null;
        }
    }
} 