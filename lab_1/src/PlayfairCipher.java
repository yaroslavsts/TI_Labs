import java.util.*;

// Шифр Плейфера с 4 шифрующими таблицами (английский язык), последовательное шифрование текста четырьмя матрицами
public class PlayfairCipher {
    private static final String ALPHABET = "ABCDEFGHIKLMNOPQRSTUVWXYZ";

    // Подготовка текста: только английские буквы, J -> I
    public String prepareText(String text) {
        return text.toUpperCase()
                .replaceAll("[^A-Za-z]", "")
                .replace("J", "I");
    }

    // Создание матрицы 5x5 из ключа
    public char[][] createMatrix(String key) {
        key = prepareText(key);
        Set<Character> used = new LinkedHashSet<>();
        for (char c : key.toCharArray()) {
            used.add(c);
        }
        StringBuilder matrixStr = new StringBuilder();
        for (Character c : used) {
            matrixStr.append(c);
        }
        for (char c : ALPHABET.toCharArray()) {
            if (!used.contains(c)) {
                matrixStr.append(c);
            }
        }

        char[][] matrix = new char[5][5];
        for (int i = 0; i < 25; i++) {
            matrix[i / 5][i % 5] = matrixStr.charAt(i);
        }
        return matrix;
    }

    // Создание биграмм с вставкой X между одинаковыми буквами и X между XX
    public List<String> createBigrams(String text) {
        text = prepareText(text);
        List<Character> result = new ArrayList<>();
        char[] chars = text.toCharArray();

        for (int i = 0; i < chars.length; i++) {
            result.add(chars[i]);
            if (i + 1 < chars.length) {
                if (chars[i] == chars[i + 1]) {
                    result.add(chars[i] == 'X' ? 'Z' : 'X');
                }
            }
        }
        if (result.size() % 2 == 1) {
            result.add('X');
        }

        List<String> pairs = new ArrayList<>();
        for (int i = 0; i < result.size(); i += 2) {
            pairs.add("" + result.get(i) + result.get(i + 1));
        }
        return pairs;
    }

    private int[] findPosition(char[][] matrix, char c) {
        for (int i = 0; i < 5; i++) {
            for (int j = 0; j < 5; j++) {
                if (matrix[i][j] == c) return new int[]{i, j};
            }
        }
        return null;
    }

    // Шифрование одной биграммы одной матрицей
    private String encryptPair(char[][] matrix, String pair) {
        int[] pos1 = findPosition(matrix, pair.charAt(0));
        int[] pos2 = findPosition(matrix, pair.charAt(1));
        if (pos1 == null || pos2 == null) return pair;

        if (pos1[0] == pos2[0]) {
            return "" + matrix[pos1[0]][(pos1[1] + 1) % 5] + matrix[pos2[0]][(pos2[1] + 1) % 5];
        } else if (pos1[1] == pos2[1]) {
            return "" + matrix[(pos1[0] + 1) % 5][pos1[1]] + matrix[(pos2[0] + 1) % 5][pos2[1]];
        } else {
            return "" + matrix[pos1[0]][pos2[1]] + matrix[pos2[0]][pos1[1]];
        }
    }

    // Дешифрование одной биграммы одной матрицей
    private String decryptPair(char[][] matrix, String pair) {
        int[] pos1 = findPosition(matrix, pair.charAt(0));
        int[] pos2 = findPosition(matrix, pair.charAt(1));
        if (pos1 == null || pos2 == null) return pair;

        if (pos1[0] == pos2[0]) {
            return "" + matrix[pos1[0]][(pos1[1] + 4) % 5] + matrix[pos2[0]][(pos2[1] + 4) % 5];
        } else if (pos1[1] == pos2[1]) {
            return "" + matrix[(pos1[0] + 4) % 5][pos1[1]] + matrix[(pos2[0] + 4) % 5][pos2[1]];
        } else {
            return "" + matrix[pos1[0]][pos2[1]] + matrix[pos2[0]][pos1[1]];
        }
    }

    // Шифрование: текст -> матрица1 -> матрица2 -> матрица3 -> матрица4
    public String encrypt(String text, String key1, String key2, String key3, String key4) {
        if (key1 == null || key2 == null || key3 == null || key4 == null ||
                key1.isEmpty() || key2.isEmpty() || key3.isEmpty() || key4.isEmpty()) {
            throw new IllegalArgumentException("Все 4 ключа обязательны");
        }
        List<String> bigrams = createBigrams(text);
        char[][] m1 = createMatrix(key1);
        char[][] m2 = createMatrix(key2);
        char[][] m3 = createMatrix(key3);
        char[][] m4 = createMatrix(key4);

        StringBuilder result = new StringBuilder();
        for (String pair : bigrams) {
            String s = encryptPair(m1, pair);
            s = encryptPair(m2, s);
            s = encryptPair(m3, s);
            s = encryptPair(m4, s);
            result.append(s);
        }
        return result.toString();
    }

    // Дешифрование: матрицы в обратном порядке
    public String decrypt(String text, String key1, String key2, String key3, String key4) {
        if (key1 == null || key2 == null || key3 == null || key4 == null ||
                key1.isEmpty() || key2.isEmpty() || key3.isEmpty() || key4.isEmpty()) {
            throw new IllegalArgumentException("Все 4 ключа обязательны");
        }
        text = prepareText(text);
        if (text.length() % 2 != 0) {
            throw new IllegalArgumentException("Шифротекст должен иметь чётную длину");
        }
        char[][] m1 = createMatrix(key1);
        char[][] m2 = createMatrix(key2);
        char[][] m3 = createMatrix(key3);
        char[][] m4 = createMatrix(key4);

        StringBuilder result = new StringBuilder();
        for (int i = 0; i < text.length(); i += 2) {
            String pair = text.substring(i, i + 2);
            String s = decryptPair(m4, pair);
            s = decryptPair(m3, s);
            s = decryptPair(m2, s);
            s = decryptPair(m1, s);
            result.append(s);
        }
        return result.toString();
    }
}
