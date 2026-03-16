import java.util.*;

// Шифр Плейфера с 4 шифрующими таблицами (английский язык), перекрёстное шифрование
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

    // Создание биграмм: разбиение на пары по 2 символа, дополнение X при нечётной длине
    public List<String> createBigrams(String text) {
        text = prepareText(text);
        if (text.length() % 2 != 0) {
            text += 'X';
        }
        List<String> pairs = new ArrayList<>();
        for (int i = 0; i < text.length(); i += 2) {
            pairs.add("" + text.charAt(i) + text.charAt(i + 1));
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

    // Перекрёстное шифрование биграммы через 4 матрицы
    // Первая буква ищется в mFind1, вторая в mFind2
    // Результат: mOut1[row1][col2] + mOut2[row2][col1]
    private String crossPair(char[][] mFind1, char[][] mFind2, char[][] mOut1, char[][] mOut2, String pair) {
        int[] pos1 = findPosition(mFind1, pair.charAt(0));
        int[] pos2 = findPosition(mFind2, pair.charAt(1));
        if (pos1 == null || pos2 == null) return pair;
        return "" + mOut1[pos1[0]][pos2[1]] + mOut2[pos2[0]][pos1[1]];
    }

    // Шифрование: поиск в матрицах 1 и 4, вывод из матриц 2 и 3
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
            result.append(crossPair(m1, m4, m2, m3, pair));
        }
        return result.toString();
    }

    // Дешифрование: поиск в матрицах 2 и 3, вывод из матриц 1 и 4
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
            result.append(crossPair(m2, m3, m1, m4, pair));
        }
        return result.toString();
    }
}
