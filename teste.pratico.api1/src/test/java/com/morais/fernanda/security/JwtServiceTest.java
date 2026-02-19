package com.morais.fernanda.security;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class JwtServiceTest {

    @Test
    void generateToken_deveGerarTokenValidoEExtrairUsername() {
        JwtService jwtService = new JwtService(
                "01234567890123456789012345678901",
                60
        );

        String token = jwtService.generateToken("usuario");

        assertNotNull(token);
        assertFalse(token.isBlank());
        assertTrue(jwtService.isTokenValid(token));
        assertEquals("usuario", jwtService.extractUsername(token));
    }

    @Test
    void isTokenValid_quandoExpirado_deveRetornarFalse() throws InterruptedException {
        JwtService jwtService = new JwtService(
                "01234567890123456789012345678901",
                0
        );

        String token = jwtService.generateToken("usuario");

        Thread.sleep(5);

        assertFalse(jwtService.isTokenValid(token));
    }
}
