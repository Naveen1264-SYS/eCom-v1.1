//package com.user_service.service;
//
//import io.jsonwebtoken.Claims;
//import io.jsonwebtoken.Jwts;
//import io.jsonwebtoken.SignatureAlgorithm;
//import org.springframework.beans.factory.annotation.Value;
//import org.springframework.stereotype.Service;
//
//import java.util.Date;
//import java.util.HashMap;
//import java.util.Map;
//import java.util.function.Function;
//
//@Service
//public class JwtService {
//
//    @Value("${jwt.secret}")
//    private String secret;
//
//    @Value("${jwt.expiration}")
//    private Long expiration;
//
//    public String generateToken(String email, String role) {
//        Map<String, Object> claims = new HashMap<>();
//        claims.put("role", role);
//        return createToken(claims, email);
//    }
//
//    private String createToken(Map<String, Object> claims, String subject) {
//        return Jwts.builder()
//                .setClaims(claims)
//                .setSubject(subject)
//                .setIssuedAt(new Date(System.currentTimeMillis()))
//                .setExpiration(new Date(System.currentTimeMillis() + expiration * 1000))
//                .signWith(SignatureAlgorithm.HS256, secret)
//                .compact();
//    }
//
//    public String extractEmail(String token) {
//        return extractClaim(token, Claims::getSubject);
//    }
//
//    public String extractRole(String token) {
//        return extractClaim(token, claims -> claims.get("role", String.class));
//    }
//
//    public <T> T extractClaim(String token, Function<Claims, T> claimsResolver) {
//        final Claims claims = extractAllClaims(token);
//        return claimsResolver.apply(claims);
//    }
//
//    private Claims extractAllClaims(String token) {
//        return Jwts.parser().setSigningKey(secret).parseClaimsJws(token).getBody();
//    }
//
//    public Boolean isTokenValid(String token, String email) {
//        final String extractedEmail = extractEmail(token);
//        return (extractedEmail.equals(email) && !isTokenExpired(token));
//    }
//
//    private Boolean isTokenExpired(String token) {
//        return extractExpiration(token).before(new Date());
//    }
//
//    private Date extractExpiration(String token) {
//        return extractClaim(token, Claims::getExpiration);
//    }
//}

//package com.user_service.service;
//
//import com.user_service.entity.TokenBlacklist;
//import com.user_service.repository.TokenBlacklistRepository;
//import io.jsonwebtoken.*;
//import io.jsonwebtoken.security.Keys;
//import org.slf4j.Logger;
//import org.slf4j.LoggerFactory;
//import org.springframework.beans.factory.annotation.Value;
//import org.springframework.stereotype.Component;
//
//import javax.crypto.SecretKey;
//import java.time.LocalDateTime;
//import java.time.ZoneId;
//import java.util.Base64;
//import java.util.Date;
//import java.util.HashMap;
//import java.util.Map;
//
//@Component
//public class JwtService {
//    private static final Logger logger = LoggerFactory.getLogger(JwtService.class);
//
//    private final SecretKey secretKey;
//    private final long expirationMs;
//    private final TokenBlacklistRepository tokenBlacklistRepository;
//
//    public JwtService(@Value("${jwt.secret}") String base64Secret,
//                      @Value("${jwt.expiration:86400000}") long expirationMs,
//                      TokenBlacklistRepository tokenBlacklistRepository) {
//        if (base64Secret == null || base64Secret.trim().isEmpty()) {
//            throw new IllegalArgumentException("JWT secret cannot be null or empty");
//        }
//        try {
//            byte[] decodedKey = Base64.getDecoder().decode(base64Secret);
//            if (decodedKey.length < 32) {
//                throw new IllegalArgumentException("JWT secret is too short: " + decodedKey.length + " bytes, expected at least 32 bytes for HS256");
//            }
//            this.secretKey = Keys.hmacShaKeyFor(decodedKey);
//            logger.info("JWT secret key initialized successfully, length: {} bytes", decodedKey.length);
//        } catch (IllegalArgumentException e) {
//            throw new IllegalArgumentException("Invalid JWT secret: must be a valid Base64-encoded string", e);
//        }
//        this.expirationMs = expirationMs;
//        this.tokenBlacklistRepository = tokenBlacklistRepository;
//    }
//
//    public String generateToken(String email, String role) {
//        Map<String, Object> claims = new HashMap<>();
//        claims.put("role", role);
//        try {
//            return Jwts.builder()
//                    .setSubject(email)
//                    .setClaims(claims)
//                    .setIssuedAt(new Date())
//                    .setExpiration(new Date(System.currentTimeMillis() + expirationMs))
//                    .signWith(secretKey, SignatureAlgorithm.HS256)
//                    .compact();
//        } catch (Exception e) {
//            logger.error("Failed to create JWT token for email: {}, error: {}", email, e.getMessage());
//            throw e;
//        }
//    }
//
//    public void blacklistToken(String token) {
//        try {
//            Claims claims = Jwts.parserBuilder()
//                    .setSigningKey(secretKey)
//                    .build()
//                    .parseClaimsJws(token)
//                    .getBody();
//            LocalDateTime expiryDate = claims.getExpiration()
//                    .toInstant()
//                    .atZone(ZoneId.systemDefault())
//                    .toLocalDateTime();
//            TokenBlacklist blacklistedToken = new TokenBlacklist(token, expiryDate);
//            tokenBlacklistRepository.save(blacklistedToken);
//            logger.info("Token blacklisted successfully for email: {}", claims.getSubject());
//        } catch (JwtException e) {
//            logger.warn("Failed to blacklist invalid or expired token: {}", e.getMessage());
//            throw new IllegalArgumentException("Invalid token", e);
//        }
//    }
//
//    public boolean isTokenBlacklisted(String token) {
//        return tokenBlacklistRepository.findByToken(token).isPresent();
//    }
//
//    public Map<String, Object> getClaimsFromToken(String token) {
//        try {
//            Claims claims = Jwts.parserBuilder()
//                    .setSigningKey(secretKey)
//                    .build()
//                    .parseClaimsJws(token)
//                    .getBody();
//            Map<String, Object> result = new HashMap<>();
//            result.put("email", claims.getSubject());
//            result.put("role", claims.get("role", String.class));
//            return result;
//        } catch (ExpiredJwtException e) {
//            logger.warn("Token expired for email: {}", e.getClaims().getSubject());
//            throw e;
//        } catch (JwtException e) {
//            logger.error("Invalid JWT token: {}", e.getMessage());
//            throw e;
//        }
//    }
//
//    public String getEmailFromToken(String token) {
//        return (String) getClaimsFromToken(token).get("email");
//    }
//
//    public String getRoleFromToken(String token) {
//        return (String) getClaimsFromToken(token).get("role");
//    }
//
//    public boolean validateToken(String token) {
//        if (isTokenBlacklisted(token)) {
//            logger.warn("Token is blacklisted");
//            return false;
//        }
//        try {
//            Jwts.parserBuilder().setSigningKey(secretKey).build().parseClaimsJws(token);
//            return true;
//        } catch (ExpiredJwtException e) {
//            logger.warn("Token expired: {}", e.getMessage());
//            return false;
//        } catch (UnsupportedJwtException | MalformedJwtException | SignatureException e) {
//            logger.error("Invalid JWT token: {}", e.getMessage());
//            return false;
//        } catch (Exception e) {
//            logger.error("JWT validation error: {}", e.getMessage());
//            return false;
//        }
//    }
//}
package com.user_service.service;

import com.user_service.entity.TokenBlacklist;
import com.user_service.repository.TokenBlacklistRepository;
import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Base64;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

@Component
public class JwtService {
    private static final Logger logger = LoggerFactory.getLogger(JwtService.class);

    private final SecretKey secretKey;
    private final long expirationMs;
    private final TokenBlacklistRepository tokenBlacklistRepository;

    public JwtService(@Value("${jwt.secret}") String base64Secret,
                      @Value("${jwt.expiration:86400000}") long expirationMs,
                      TokenBlacklistRepository tokenBlacklistRepository) {
        if (base64Secret == null || base64Secret.trim().isEmpty()) {
            throw new IllegalArgumentException("JWT secret cannot be null or empty");
        }
        try {
            byte[] decodedKey = Base64.getDecoder().decode(base64Secret);
            if (decodedKey.length < 32) {
                throw new IllegalArgumentException("JWT secret is too short: " + decodedKey.length + " bytes, expected at least 32 bytes for HS256");
            }
            this.secretKey = Keys.hmacShaKeyFor(decodedKey);
            logger.info("JWT secret key initialized successfully, length: {} bytes", decodedKey.length);
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Invalid JWT secret: must be a valid Base64-encoded string", e);
        }
        this.expirationMs = expirationMs;
        this.tokenBlacklistRepository = tokenBlacklistRepository;
    }

    public String generateToken(String email, String role) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("role", role);
        try {
            String token = Jwts.builder()
                    .setSubject(email)
                    .setClaims(claims)
                    .setIssuedAt(new Date())
                    .setExpiration(new Date(System.currentTimeMillis() + expirationMs))
                    .signWith(secretKey, SignatureAlgorithm.HS256)
                    .compact();
            logger.debug("Generated JWT token for email: {}", email);
            return token;
        } catch (Exception e) {
            logger.error("Failed to create JWT token for email: {}, error: {}", email, e.getMessage());
            throw e;
        }
    }

    public void blacklistToken(String token) {
        try {
            Claims claims = Jwts.parserBuilder()
                    .setSigningKey(secretKey)
                    .build()
                    .parseClaimsJws(token)
                    .getBody();
            LocalDateTime expiryDate = claims.getExpiration()
                    .toInstant()
                    .atZone(ZoneId.systemDefault())
                    .toLocalDateTime();
            TokenBlacklist blacklistedToken = new TokenBlacklist(token, expiryDate);
            tokenBlacklistRepository.save(blacklistedToken);
            logger.info("Token blacklisted successfully for email: {}", claims.getSubject());
        } catch (SignatureException e) {
            logger.warn("Failed to blacklist token due to invalid signature: {}", e.getMessage());
            throw new IllegalArgumentException("Invalid token signature", e);
        } catch (JwtException e) {
            logger.warn("Failed to blacklist invalid or expired token: {}", e.getMessage());
            throw new IllegalArgumentException("Invalid token", e);
        }
    }

    public boolean isTokenBlacklisted(String token) {
        boolean isBlacklisted = tokenBlacklistRepository.findByToken(token).isPresent();
        if (isBlacklisted) {
            logger.warn("Token is blacklisted");
        }
        return isBlacklisted;
    }

    public Map<String, Object> getClaimsFromToken(String token) {
        try {
            Claims claims = Jwts.parserBuilder()
                    .setSigningKey(secretKey)
                    .build()
                    .parseClaimsJws(token)
                    .getBody();
            Map<String, Object> result = new HashMap<>();
            result.put("email", claims.getSubject());
            result.put("role", claims.get("role", String.class));
            logger.debug("Extracted claims from token for email: {}", claims.getSubject());
            return result;
        } catch (SignatureException e) {
            logger.error("Invalid JWT signature for token: {}", e.getMessage());
            throw new IllegalArgumentException("Invalid token signature", e);
        } catch (ExpiredJwtException e) {
            logger.warn("Token expired for email: {}", e.getClaims().getSubject());
            throw e;
        } catch (JwtException e) {
            logger.error("Invalid JWT token: {}", e.getMessage());
            throw e;
        }
    }

    public String getEmailFromToken(String token) {
        return (String) getClaimsFromToken(token).get("email");
    }

    public String getRoleFromToken(String token) {
        return (String) getClaimsFromToken(token).get("role");
    }

    public boolean validateToken(String token) {
        if (isTokenBlacklisted(token)) {
            return false;
        }
        try {
            Jwts.parserBuilder().setSigningKey(secretKey).build().parseClaimsJws(token);
            logger.debug("Token validated successfully");
            return true;
        } catch (SignatureException e) {
            logger.error("Invalid JWT signature: {}", e.getMessage());
            return false;
        } catch (ExpiredJwtException e) {
            logger.warn("Token expired: {}", e.getMessage());
            return false;
        } catch (UnsupportedJwtException | MalformedJwtException | IllegalArgumentException e) {
            logger.error("Invalid JWT token: {}", e.getMessage());
            return false;
        } catch (Exception e) {
            logger.error("JWT validation error: {}", e.getMessage());
            return false;
        }
    }
}