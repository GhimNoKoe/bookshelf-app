package com.bookshelf.user.service;

import com.bookshelf.user.config.JwtProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class JwtServiceTest {

    /**
     * Test-only 256-bit base64 key — never used in production.
     * Matches the default in application.yml so the same value can be referenced
     * from test resources when Spring context is involved.
     */
    private static final String TEST_SECRET =
            "dGVzdC1zZWNyZXQta2V5LW11c3QtYmUtYXQtbGVhc3QtMjU2LWJpdHMtZm9yLUhNQUMtU0hBMjU2";

    private JwtService jwtService;

    @BeforeEach
    void setUp() {
        JwtProperties props = new JwtProperties();
        props.setSecret(TEST_SECRET);
        props.setExpiration(3_600_000L);
        jwtService = new JwtService(props);
    }

    @Test
    void generateToken_returnsNonBlankToken() {
        assertThat(jwtService.generateToken("alice")).isNotBlank();
    }

    @Test
    void extractUsername_returnsSubject() {
        String token = jwtService.generateToken("alice");
        assertThat(jwtService.extractUsername(token)).isEqualTo("alice");
    }

    @Test
    void validateToken_trueForFreshToken() {
        assertThat(jwtService.validateToken(jwtService.generateToken("alice"))).isTrue();
    }

    @Test
    void validateToken_falseForGarbage() {
        assertThat(jwtService.validateToken("not.a.jwt")).isFalse();
    }

    @Test
    void validateToken_falseForTamperedSignature() {
        String token = jwtService.generateToken("alice");
        int lastDot = token.lastIndexOf('.');
        String corruptedToken = token.substring(0, lastDot + 1)
                + (token.charAt(lastDot + 1) == 'A' ? 'B' : 'A')
                + token.substring(lastDot + 2);
        assertThat(jwtService.validateToken(corruptedToken)).isFalse();
    }

    @Test
    void validateToken_falseForExpiredToken() {
        JwtProperties props = new JwtProperties();
        props.setSecret(TEST_SECRET);
        props.setExpiration(-1L); // expiry in the past
        JwtService shortLived = new JwtService(props);

        String token = shortLived.generateToken("alice");
        assertThat(shortLived.validateToken(token)).isFalse();
    }

    @Test
    void tokensForDifferentUsernamesDiffer() {
        assertThat(jwtService.generateToken("alice"))
                .isNotEqualTo(jwtService.generateToken("bob"));
    }
}
