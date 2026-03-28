// Шифр Виженера с прямым ключом (русский язык), ключ повторяется для совпадения длины текста
public class VigenereCipher {

    // Русский алфавит с Ё (33 буквы)
    private static final String ALPHABET_FULL = "АБВГДЕЁЖЗИЙКЛМНОПРСТУФХЦЧШЩЪЫЬЭЮЯ";

    // Подготовка текста: только русские буквы (включая Ё)
    public String prepareText(String text) {
        return text.toUpperCase().replaceAll("[^А-Яа-яЁё]", "");
    }

    public String prepareKey(String key) {
        String k = key.toUpperCase().replaceAll("[^А-Яа-яЁё]", "");
        if (k.isEmpty()) throw new IllegalArgumentException("Ключ должен содержать русские буквы");
        return k;
    }

    private int charToIndex(char c) {
        return ALPHABET_FULL.indexOf(Character.toUpperCase(c));
    }

    private char indexToChar(int idx) {
        return ALPHABET_FULL.charAt((idx + ALPHABET_FULL.length()) % ALPHABET_FULL.length());
    }

    // Шифрование Виженера (прямой ключ)
    public String encrypt(String text, String key) {
        key = prepareKey(key);
        text = prepareText(text);
        StringBuilder result = new StringBuilder();
        int ki = 0;
        for (char c : text.toCharArray()) {
            int ti = charToIndex(c);
            int kiVal = charToIndex(key.charAt(ki % key.length()));
            result.append(indexToChar(ti + kiVal));
            ki++;
        }
        return result.toString();
    }

    // Дешифрование Виженера
    public String decrypt(String text, String key) {
        key = prepareKey(key);
        text = prepareText(text);
        StringBuilder result = new StringBuilder();
        int ki = 0;
        for (char c : text.toCharArray()) {
            int ti = charToIndex(c);
            int kiVal = charToIndex(key.charAt(ki % key.length()));
            result.append(indexToChar(ti - kiVal));
            ki++;
        }
        return result.toString();
    }
}
