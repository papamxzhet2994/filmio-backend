package com.papamxzhet.filmio;

import com.papamxzhet.filmio.security.jwt.JwtUtils;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.security.Key;
import java.util.Date;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("JwtUtils Tests")
class JwtUtilsTest {

    private JwtUtils jwtUtils;
    private String testSecret;
    private long testExpirationMs;
    private String testUsername;

    @BeforeEach
    void setUp() {
        jwtUtils = new JwtUtils();
        testSecret = "mySecretKeyForTestingPurposesItShouldBeLongEnoughForHS512Algorithm";
        testExpirationMs = 86400000L; // 24ч
        testUsername = "testUser";

        ReflectionTestUtils.setField(jwtUtils, "jwtSecret", testSecret);
        ReflectionTestUtils.setField(jwtUtils, "jwtExpirationMs", testExpirationMs);
    }

    @Test
    @DisplayName("Должен успешно генерировать JWT токен")
    void generateJwtToken_WithValidUsername_ShouldGenerateToken() {
        String token = jwtUtils.generateJwtToken(testUsername);

        assertNotNull(token);
        assertFalse(token.isEmpty());
        assertEquals(3, token.split("\\.").length);
    }

    @Test
    @DisplayName("Должен генерировать разные токены для одного пользователя")
    void generateJwtToken_MultipleCalls_ShouldGenerateDifferentTokens() throws InterruptedException {
        String token1 = jwtUtils.generateJwtToken(testUsername);

        Thread.sleep(1000);

        String token2 = jwtUtils.generateJwtToken(testUsername);

        assertNotEquals(token1, token2);
    }

    @Test
    @DisplayName("Должен извлекать имя пользователя из валидного токена")
    void getUsernameFromJwtToken_WithValidToken_ShouldReturnUsername() {
        String token = jwtUtils.generateJwtToken(testUsername);

        String extractedUsername = jwtUtils.getUsernameFromJwtToken(token);

        assertEquals(testUsername, extractedUsername);
    }

    @Test
    @DisplayName("Должен выбрасывать исключение при извлечении имени из невалидного токена")
    void getUsernameFromJwtToken_WithInvalidToken_ShouldThrowException() {
        String invalidToken = "invalid.jwt.token";

        assertThrows(JwtException.class, () -> {
            jwtUtils.getUsernameFromJwtToken(invalidToken);
        });
    }

    @Test
    @DisplayName("Должен успешно валидировать корректный токен")
    void validateJwtToken_WithValidToken_ShouldReturnTrue() {
        String token = jwtUtils.generateJwtToken(testUsername);

        boolean isValid = jwtUtils.validateJwtToken(token);

        assertTrue(isValid);
    }

    @Test
    @DisplayName("Должен выбрасывать исключение для невалидного токена")
    void validateJwtToken_WithInvalidToken_ShouldThrowException() {
        String invalidToken = "invalid.jwt.token";

        JwtException exception = assertThrows(JwtException.class, () -> {
            jwtUtils.validateJwtToken(invalidToken);
        });

        assertTrue(exception.getMessage().contains("Invalid JWT token"));
    }

    @Test
    @DisplayName("Должен выбрасывать исключение для пустого токена")
    void validateJwtToken_WithEmptyToken_ShouldThrowException() {
        String emptyToken = "";

        JwtException exception = assertThrows(JwtException.class, () -> {
            jwtUtils.validateJwtToken(emptyToken);
        });

        assertTrue(exception.getMessage().contains("Invalid JWT token"));
    }

    @Test
    @DisplayName("Должен выбрасывать исключение для null токена")
    void validateJwtToken_WithNullToken_ShouldThrowException() {
        JwtException exception = assertThrows(JwtException.class, () -> {
            jwtUtils.validateJwtToken(null);
        });

        assertTrue(exception.getMessage().contains("Invalid JWT token"));
    }

    @Test
    @DisplayName("Должен выбрасывать исключение для токена с неправильной подписью")
    void validateJwtToken_WithWrongSignature_ShouldThrowException() {
        String differentSecret = "differentSecretKeyForTestingPurposesItShouldBeLongEnoughForHS512";
        Key differentKey = Keys.hmacShaKeyFor(differentSecret.getBytes());

        String tokenWithWrongSignature = Jwts.builder()
                .setSubject(testUsername)
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + testExpirationMs))
                .signWith(differentKey)
                .compact();

        JwtException exception = assertThrows(JwtException.class, () -> {
            jwtUtils.validateJwtToken(tokenWithWrongSignature);
        });

        assertTrue(exception.getMessage().contains("Invalid JWT token"));
    }

    @Test
    @DisplayName("Должен выбрасывать исключение для истекшего токена")
    void validateJwtToken_WithExpiredToken_ShouldThrowException() {
        Key signingKey = Keys.hmacShaKeyFor(testSecret.getBytes());
        String expiredToken = Jwts.builder()
                .setSubject(testUsername)
                .setIssuedAt(new Date(System.currentTimeMillis() - 5000))
                .setExpiration(new Date(System.currentTimeMillis() - 1000))
                .signWith(signingKey)
                .compact();

        JwtException exception = assertThrows(JwtException.class, () -> {
            jwtUtils.validateJwtToken(expiredToken);
        });

        assertTrue(exception.getMessage().contains("Invalid JWT token"));
    }

    @Test
    @DisplayName("Должен генерировать токен с правильным временем истечения")
    void generateJwtToken_ShouldSetCorrectExpiration() {
        long beforeGeneration = System.currentTimeMillis();
        String token = jwtUtils.generateJwtToken(testUsername);
        long afterGeneration = System.currentTimeMillis();

        Key signingKey = Keys.hmacShaKeyFor(testSecret.getBytes());
        Date expiration = Jwts.parserBuilder()
                .setSigningKey(signingKey)
                .build()
                .parseClaimsJws(token)
                .getBody()
                .getExpiration();

        long expectedExpirationMin = beforeGeneration + testExpirationMs - 1000;
        long expectedExpirationMax = afterGeneration + testExpirationMs + 1000;

        assertTrue(expiration.getTime() >= expectedExpirationMin,
                "Expiration time should be greater than or equal to " + expectedExpirationMin + " but was " + expiration.getTime());
        assertTrue(expiration.getTime() <= expectedExpirationMax,
                "Expiration time should be less than or equal to " + expectedExpirationMax + " but was " + expiration.getTime());
    }

    @Test
    @DisplayName("Должен генерировать токен с правильным subject")
    void generateJwtToken_ShouldSetCorrectSubject() {
        String token = jwtUtils.generateJwtToken(testUsername);

        Key signingKey = Keys.hmacShaKeyFor(testSecret.getBytes());
        String subject = Jwts.parserBuilder()
                .setSigningKey(signingKey)
                .build()
                .parseClaimsJws(token)
                .getBody()
                .getSubject();

        assertEquals(testUsername, subject);
    }

    @Test
    @DisplayName("Должен генерировать токен с установленным временем создания")
    void generateJwtToken_ShouldSetIssuedAt() {
        long beforeGeneration = System.currentTimeMillis();
        String token = jwtUtils.generateJwtToken(testUsername);
        long afterGeneration = System.currentTimeMillis();

        Key signingKey = Keys.hmacShaKeyFor(testSecret.getBytes());
        Date issuedAt = Jwts.parserBuilder()
                .setSigningKey(signingKey)
                .build()
                .parseClaimsJws(token)
                .getBody()
                .getIssuedAt();

        assertTrue(issuedAt.getTime() >= beforeGeneration - 1000, // допуск 1 секунда
                "IssuedAt time should be greater than or equal to " + (beforeGeneration - 1000) + " but was " + issuedAt.getTime());
        assertTrue(issuedAt.getTime() <= afterGeneration + 1000, // допуск 1 секунда
                "IssuedAt time should be less than or equal to " + (afterGeneration + 1000) + " but was " + issuedAt.getTime());
    }

    @Test
    @DisplayName("Должен обрабатывать специальные символы в имени пользователя")
    void generateAndExtractUsername_WithSpecialCharacters_ShouldWorkCorrectly() {
        String usernameWithSpecialChars = "user@domain.com";

        String token = jwtUtils.generateJwtToken(usernameWithSpecialChars);
        String extractedUsername = jwtUtils.getUsernameFromJwtToken(token);

        assertEquals(usernameWithSpecialChars, extractedUsername);
    }

    @Test
    @DisplayName("Должен обрабатывать длинные имена пользователей")
    void generateAndExtractUsername_WithLongUsername_ShouldWorkCorrectly() {
        String longUsername = "very_long_username_that_contains_many_characters_to_test_token_generation";

        String token = jwtUtils.generateJwtToken(longUsername);
        String extractedUsername = jwtUtils.getUsernameFromJwtToken(token);

        assertEquals(longUsername, extractedUsername);
    }

    @Test
    @DisplayName("Должен генерировать токен, который можно валидировать")
    void generateAndValidate_ShouldWorkTogether() {
        String token = jwtUtils.generateJwtToken(testUsername);

        assertTrue(jwtUtils.validateJwtToken(token));
        assertEquals(testUsername, jwtUtils.getUsernameFromJwtToken(token));
    }

    @Test
    @DisplayName("Должен обрабатывать пустое имя пользователя")
    void generateJwtToken_WithEmptyUsername_ShouldGenerateToken() {
        String emptyUsername = "";

        String token = jwtUtils.generateJwtToken(emptyUsername);
        String extractedUsername = jwtUtils.getUsernameFromJwtToken(token);

        assertTrue(extractedUsername == null || extractedUsername.isEmpty());
    }

    @Test
    @DisplayName("Должен обрабатывать null имя пользователя")
    void generateJwtToken_WithNullUsername_ShouldHandleGracefully() {
        String token = jwtUtils.generateJwtToken(null);

        assertNotNull(token);
        assertFalse(token.isEmpty());

        String extractedUsername = jwtUtils.getUsernameFromJwtToken(token);
        assertNull(extractedUsername);
    }

    @Test
    @DisplayName("Должен обрабатывать имя пользователя только из пробелов")
    void generateJwtToken_WithWhitespaceUsername_ShouldHandleGracefully() {
        String whitespaceUsername = "   ";

        String token = jwtUtils.generateJwtToken(whitespaceUsername);
        String extractedUsername = jwtUtils.getUsernameFromJwtToken(token);

        assertTrue(extractedUsername == null || extractedUsername.equals(whitespaceUsername));
    }

    @Test
    @DisplayName("Должен обрабатывать имя пользователя с Unicode символами")
    void generateJwtToken_WithUnicodeUsername_ShouldWorkCorrectly() {
        String unicodeUsername = "пользователь123";

        String token = jwtUtils.generateJwtToken(unicodeUsername);
        String extractedUsername = jwtUtils.getUsernameFromJwtToken(token);

        assertEquals(unicodeUsername, extractedUsername);
    }

    @Test
    @DisplayName("Должен обрабатывать очень короткие имена пользователей")
    void generateJwtToken_WithSingleCharacterUsername_ShouldWorkCorrectly() {
        String singleCharUsername = "a";

        String token = jwtUtils.generateJwtToken(singleCharUsername);
        String extractedUsername = jwtUtils.getUsernameFromJwtToken(token);

        assertEquals(singleCharUsername, extractedUsername);
    }

    @Test
    @DisplayName("Должен обрабатывать имена пользователей с цифрами")
    void generateJwtToken_WithNumericUsername_ShouldWorkCorrectly() {
        String numericUsername = "user123";

        String token = jwtUtils.generateJwtToken(numericUsername);
        String extractedUsername = jwtUtils.getUsernameFromJwtToken(token);

        assertEquals(numericUsername, extractedUsername);
    }

    @Test
    @DisplayName("Должен генерировать валидный токен даже с граничными значениями времени")
    void generateJwtToken_WithDifferentExpirationTimes_ShouldWorkCorrectly() {
        ReflectionTestUtils.setField(jwtUtils, "jwtExpirationMs", 1000L);

        String token = jwtUtils.generateJwtToken(testUsername);

        assertNotNull(token);
        assertTrue(jwtUtils.validateJwtToken(token));
        assertEquals(testUsername, jwtUtils.getUsernameFromJwtToken(token));
    }

    @Test
    @DisplayName("Должен корректно работать с разными алгоритмами подписи")
    void generateJwtToken_ShouldUseHS512Algorithm() {
        String token = jwtUtils.generateJwtToken(testUsername);

        assertNotNull(token);

        Key signingKey = Keys.hmacShaKeyFor(testSecret.getBytes());
        assertDoesNotThrow(() -> {
            Jwts.parserBuilder()
                    .setSigningKey(signingKey)
                    .build()
                    .parseClaimsJws(token);
        });
    }
}