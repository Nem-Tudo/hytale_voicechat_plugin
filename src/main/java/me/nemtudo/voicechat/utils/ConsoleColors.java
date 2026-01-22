package me.nemtudo.voicechat.utils;

public class ConsoleColors {
    // Reset
    public static final String RESET = "\u001B[0m";

    // Cores normais
    public static final String BLACK = "\u001B[30m";
    public static final String RED = "\u001B[31m";
    public static final String GREEN = "\u001B[32m";
    public static final String YELLOW = "\u001B[33m";
    public static final String BLUE = "\u001B[34m";
    public static final String PURPLE = "\u001B[35m";
    public static final String CYAN = "\u001B[36m";
    public static final String WHITE = "\u001B[37m";

    // Cores brilhantes
    public static final String BRIGHT_GREEN = "\u001B[92m";
    public static final String BRIGHT_YELLOW = "\u001B[93m";
    public static final String BRIGHT_BLUE = "\u001B[94m";
    public static final String BRIGHT_CYAN = "\u001B[96m";

    // Estilos
    public static final String BOLD = "\u001B[1m";
    public static final String UNDERLINE = "\u001B[4m";

    // Method utilitário
    public static String green(String text) {
        return GREEN + text + RESET;
    }

    public static String brightGreen(String text) {
        return BRIGHT_GREEN + text + RESET;
    }

    public static String success(String text) {
        return BRIGHT_GREEN + "✓ " + text + RESET;
    }

    public static String error(String text) {
        return RED + "✗ " + text + RESET;
    }
}