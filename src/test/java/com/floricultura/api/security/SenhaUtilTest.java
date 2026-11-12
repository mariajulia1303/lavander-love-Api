package com.floricultura.api.security;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

// testes do hash de senha usado no login
class SenhaUtilTest {

    @Test
    void senhaCertaConfere() {
        String hash = SenhaUtil.gerarHash("minhaSenha123");
        assertTrue(SenhaUtil.confere("minhaSenha123", hash));
    }

    @Test
    void senhaErradaNaoConfere() {
        String hash = SenhaUtil.gerarHash("minhaSenha123");
        assertFalse(SenhaUtil.confere("outraSenha", hash));
    }

    @Test
    void mesmaSenhaGeraHashesDiferentes() {
        // por causa do "sal" aleatorio
        assertNotEquals(SenhaUtil.gerarHash("abc123"), SenhaUtil.gerarHash("abc123"));
    }

    @Test
    void naoGuardaASenhaEmTexto() {
        assertFalse(SenhaUtil.gerarHash("minhaSenha123").contains("minhaSenha123"));
    }
}
