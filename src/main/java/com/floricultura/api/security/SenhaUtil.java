package com.floricultura.api.security;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.spec.InvalidKeySpecException;
import java.util.Base64;

import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;

// guarda senha do jeito certo: NUNCA o texto da senha, so um hash lento com "sal" (PBKDF2, que ja vem no Java).
//  - sal: 16 bytes aleatorios por usuario -> duas pessoas com a mesma senha ficam com hashes diferentes
//  - 120 mil iteracoes: deixa cada tentativa lenta, o que atrapalha quem tenta adivinhar senha no chute
// formato salvo no banco: pbkdf2$<iteracoes>$<sal em base64>$<hash em base64>
public final class SenhaUtil {

    private static final String ALGORITMO = "PBKDF2WithHmacSHA256";
    private static final int ITERACOES = 120_000;
    private static final int TAMANHO_BITS = 256;
    private static final SecureRandom RANDOM = new SecureRandom();

    private SenhaUtil() {
    }

    public static String gerarHash(String senha) {
        byte[] sal = new byte[16];
        RANDOM.nextBytes(sal);
        byte[] hash = pbkdf2(senha.toCharArray(), sal, ITERACOES);
        Base64.Encoder b64 = Base64.getEncoder();
        return "pbkdf2$" + ITERACOES + "$" + b64.encodeToString(sal) + "$" + b64.encodeToString(hash);
    }

    public static boolean confere(String senha, String salvo) {
        if (senha == null || salvo == null) {
            return false;
        }
        String[] partes = salvo.split("\\$");
        if (partes.length != 4 || !"pbkdf2".equals(partes[0])) {
            return false;
        }
        int iteracoes = Integer.parseInt(partes[1]);
        byte[] sal = Base64.getDecoder().decode(partes[2]);
        byte[] esperado = Base64.getDecoder().decode(partes[3]);
        byte[] calculado = pbkdf2(senha.toCharArray(), sal, iteracoes);
        // comparacao em tempo constante (nao "vaza" pelo tempo quantos bytes bateram)
        return MessageDigest.isEqual(esperado, calculado);
    }

    private static byte[] pbkdf2(char[] senha, byte[] sal, int iteracoes) {
        try {
            PBEKeySpec spec = new PBEKeySpec(senha, sal, iteracoes, TAMANHO_BITS);
            return SecretKeyFactory.getInstance(ALGORITMO).generateSecret(spec).getEncoded();
        } catch (NoSuchAlgorithmException | InvalidKeySpecException e) {
            throw new IllegalStateException("PBKDF2 indisponivel", e);
        }
    }
}
