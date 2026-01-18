package me.nemtudo.voicechat.utils;

public class GenerateCode {

    /**
     * Gera um código de 6 dígitos a partir de duas strings de até 5 letras.
     * Se a string tiver mais de 5 letras, descarta o restante.
     * Algoritmo extremamente leve: apenas operações básicas (soma, multiplicação, XOR).
     * Complexidade: O(n) onde n é o tamanho das strings (10 caracteres = 10 iterações).
     */
    public static String createCode(String str1, String str2) {
        // Constantes primas pequenas para mixing
        final int P1 = 31;
        final int P2 = 37;
        final int MOD = 1000000; // Para 6 dígitos (000000-999999)

        // Limita cada string aos ÚLTIMOS 5 caracteres (ou menos, se a string for menor)
        String s1 = str1.length() > 5 ? str1.substring(str1.length() - 5) : str1;
        String s2 = str2.length() > 5 ? str2.substring(str2.length() - 5) : str2;

        // Combina as strings em minúsculas
        String combined = (s1 + s2).toLowerCase();

        int h = 0;
        int xor = 0;

        // Um único loop: muito eficiente
        for (int i = 0; i < combined.length(); i++) {
            char c = combined.charAt(i);

            // Hash acumulativo simples
            h = (h * P1 + c * (i + 1)) % MOD;

            // XOR para efeito avalanche
            xor ^= (c * P2 * (i + 1));
        }

        // Combina os dois valores
        int result = (h ^ xor) % MOD;

        // Mixing final leve
        result = (result * P1) % MOD;

        // Garante 6 dígitos com zeros à esquerda
        return String.format("%06d", result);
    }
}