package me.nemtudo.voicechat.utils;

public class VersionComparator {

    /**
     * Compara a versão atual com a última versão estável.
     * <p>
     * Estados possíveis:
     * - SAME_VERSION: versões idênticas
     * - BEHIND_LAST_PATCH: versão atual atrasada apenas no último segmento (.x)
     * - BEHIND_MAJOR: versão atual atrasada em algum segmento além do último
     * - AHEAD_LAST_PATCH: versão atual à frente apenas no último segmento (.x)
     * - AHEAD_MAJOR: versão atual à frente em algum segmento além do último
     * <p>
     * Formato esperado das versões: x.x.x.x (numérico).
     * O algoritmo é O(n), extremamente leve, usando apenas operações básicas.
     */
    public static VersionStatus compare(
            String currentVersion,
            String latestStableVersion
    ) {
        int[] current = parse(currentVersion);
        int[] latest = parse(latestStableVersion);

        int len = Math.max(current.length, latest.length);

        current = normalize(current, len);
        latest = normalize(latest, len);

        boolean diffOnlyLast = true;

        for (int i = 0; i < len; i++) {
            if (current[i] != latest[i]) {
                diffOnlyLast = (i == len - 1);
                break;
            }
        }

        if (equals(current, latest)) {
            return VersionStatus.SAME_VERSION;
        }

        int cmp = compareNumeric(current, latest);

        if (cmp < 0) {
            return diffOnlyLast
                    ? VersionStatus.BEHIND_LAST_PATCH
                    : VersionStatus.BEHIND_MAJOR;
        } else {
            return diffOnlyLast
                    ? VersionStatus.AHEAD_LAST_PATCH
                    : VersionStatus.AHEAD_MAJOR;
        }
    }

    /* ===================== helpers ===================== */

    private static int[] parse(String version) {
        String[] parts = version.split("\\.");
        int[] out = new int[parts.length];

        for (int i = 0; i < parts.length; i++) {
            out[i] = Integer.parseInt(parts[i]);
        }

        return out;
    }

    private static int[] normalize(int[] arr, int len) {
        if (arr.length == len) return arr;

        int[] out = new int[len];
        System.arraycopy(arr, 0, out, 0, arr.length);
        return out;
    }

    private static boolean equals(int[] a, int[] b) {
        for (int i = 0; i < a.length; i++) {
            if (a[i] != b[i]) return false;
        }
        return true;
    }

    private static int compareNumeric(int[] a, int[] b) {
        for (int i = 0; i < a.length; i++) {
            if (a[i] < b[i]) return -1;
            if (a[i] > b[i]) return 1;
        }
        return 0;
    }
}
