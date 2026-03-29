import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.text.AbstractDocument;
import javax.swing.text.AttributeSet;
import javax.swing.text.BadLocationException;
import javax.swing.text.DocumentFilter;
import java.awt.*;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Потоковое шифрование: гамма от LFSR(35) с P(x)=x^35+x^2+1 по методичке (лаб. №3).
 */
public class LfsrStreamCipherApp extends JFrame {
    private static final int PREVIEW_BYTES = 32;
    private static final int BYTES_PER_LINE = 8;

    private JTextField registerField;
    private JTextArea keyBitsArea;
    private JTextArea plainBinaryArea;
    private JTextArea cipherBinaryArea;
    private JLabel statusLabel;
    private JLabel fileLabel;

    private byte[] loadedPlain;
    private byte[] lastOutput;
    private Path loadedPath;

    public LfsrStreamCipherApp() {
        setTitle("Потоковое шифрование — LFSR, m = 35 (x^35 + x^2 + 1)");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(980, 820);
        setLocationRelativeTo(null);
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception ignored) {
        }

        Font mono = new Font(Font.MONOSPACED, Font.PLAIN, 12);
        Font ui = new Font("Dialog", Font.PLAIN, 13);

        JPanel main = new JPanel(new BorderLayout(10, 10));
        main.setBorder(new EmptyBorder(12, 12, 12, 12));

        JPanel north = new JPanel(new GridBagLayout());
        GridBagConstraints gc = new GridBagConstraints();
        gc.anchor = GridBagConstraints.WEST;
        gc.insets = new Insets(0, 0, 6, 8);
        gc.gridx = 0;
        gc.gridy = 0;
        gc.gridwidth = 1;
        north.add(new JLabel("Начальное состояние I (" + Lfsr35.REGISTER_BIT_LENGTH
                + " бит, слева — старший разряд):"), gc);
        gc.gridx = 1;
        gc.fill = GridBagConstraints.HORIZONTAL;
        gc.weightx = 1;
        registerField = new JTextField(50);
        registerField.setFont(mono);
        ((AbstractDocument) registerField.getDocument()).setDocumentFilter(new BinaryOnlyFilter());
        north.add(registerField, gc);

        gc.gridx = 0;
        gc.gridy = 1;
        gc.gridwidth = 2;
        gc.fill = GridBagConstraints.NONE;
        gc.weightx = 0;
        north.add(buildPresetPanel(ui), gc);

        main.add(north, BorderLayout.NORTH);

        JPanel center = new JPanel(new GridLayout(3, 1, 0, 8));
        keyBitsArea = new JTextArea(12, 72);
        plainBinaryArea = new JTextArea(12, 72);
        cipherBinaryArea = new JTextArea(12, 72);
        for (JTextArea a : new JTextArea[]{keyBitsArea, plainBinaryArea, cipherBinaryArea}) {
            a.setFont(mono);
            a.setEditable(false);
            a.setLineWrap(false);
            a.setTabSize(4);
        }
        center.add(scrollTitled(keyBitsArea, "Ключевой поток K"));
        center.add(scrollTitled(plainBinaryArea, "Исходный файл"));
        center.add(scrollTitled(cipherBinaryArea, "Результат"));
        main.add(center, BorderLayout.CENTER);

        JPanel south = new JPanel(new BorderLayout(0, 6));
        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.CENTER, 8, 0));
        JButton openBtn = new JButton("Выбрать файл…");
        JButton encBtn = new JButton("Зашифровать");
        JButton decBtn = new JButton("Расшифровать");
        JButton saveBtn = new JButton("Сохранить результат…");
        JButton clearBtn = new JButton("Очистить всё");
        for (JButton b : new JButton[]{openBtn, encBtn, decBtn, saveBtn, clearBtn}) {
            b.setFont(ui);
            buttons.add(b);
        }
        openBtn.addActionListener(e -> openFile());
        encBtn.addActionListener(e -> runCipher(true));
        decBtn.addActionListener(e -> runCipher(false));
        saveBtn.addActionListener(e -> saveResult());
        clearBtn.addActionListener(e -> clearAll());
        south.add(buttons, BorderLayout.NORTH);

        fileLabel = new JLabel("Файл не выбран");
        fileLabel.setFont(ui);
        south.add(fileLabel, BorderLayout.CENTER);

        statusLabel = new JLabel(" ");
        statusLabel.setFont(ui);
        south.add(statusLabel, BorderLayout.SOUTH);
        main.add(south, BorderLayout.SOUTH);

        setContentPane(main);
    }

    private JPanel buildPresetPanel(Font uiFont) {
        JPanel wrap = new JPanel(new BorderLayout(4, 4));
        wrap.add(new JLabel("Тестовые состояния I:"), BorderLayout.NORTH);
        JPanel grid = new JPanel(new GridLayout(2, 4, 6, 6));
        String[] labels = {
                "Все нули",
                "Все единицы",
                "Чередование 1010",
                "Чередование 0101",
                "Пары 1100",
                "Тройки 111000",
                "Единица в начале",
                "Единица в конце"
        };
        for (int i = 0; i < labels.length; i++) {
            final int presetId = i;
            JButton b = new JButton(labels[i]);
            b.setFont(uiFont);
            b.addActionListener(e -> applyPreset(presetId));
            grid.add(b);
        }
        wrap.add(grid, BorderLayout.CENTER);
        return wrap;
    }

    private void applyPreset(int id) {
        int m = Lfsr35.REGISTER_BIT_LENGTH;
        String bits = switch (id) {
            case 0 -> "0".repeat(m);
            case 1 -> "1".repeat(m);
            case 2 -> repeatPatternToLength("10", m);
            case 3 -> repeatPatternToLength("01", m);
            case 4 -> repeatPatternToLength("1100", m);
            case 5 -> repeatPatternToLength("111000", m);
            case 6 -> "1" + "0".repeat(m - 1);
            case 7 -> "0".repeat(m - 1) + "1";
            default -> "";
        };
        registerField.setText(bits);
        if (id == 0) {
            statusLabel.setText("Подсказка: все нули — недопустимое состояние для LFSR при шифровании.");
        } else {
            statusLabel.setText("Подставлено тестовое состояние: " + labelsShort(id));
        }
    }

    private static String labelsShort(int id) {
        return switch (id) {
            case 0 -> "все нули";
            case 1 -> "все единицы";
            case 2 -> "1010…";
            case 3 -> "0101…";
            case 4 -> "1100…";
            case 5 -> "111000…";
            case 6 -> "1 в начале (MSB)";
            case 7 -> "1 в конце (LSB)";
            default -> "";
        };
    }

    private static String repeatPatternToLength(String pattern, int totalBits) {
        if (pattern.isEmpty() || totalBits <= 0) {
            return "";
        }
        StringBuilder sb = new StringBuilder(totalBits);
        int pi = 0;
        while (sb.length() < totalBits) {
            sb.append(pattern.charAt(pi % pattern.length()));
            pi++;
        }
        return sb.toString();
    }

    private static JScrollPane scrollTitled(JTextArea a, String title) {
        JScrollPane sp = new JScrollPane(a);
        sp.setBorder(BorderFactory.createTitledBorder(title));
        return sp;
    }

    private void openFile() {
        JFileChooser ch = new JFileChooser();
        if (loadedPath != null && loadedPath.getParent() != null) {
            ch.setCurrentDirectory(loadedPath.getParent().toFile());
        }
        if (ch.showOpenDialog(this) != JFileChooser.APPROVE_OPTION) {
            return;
        }
        loadedPath = ch.getSelectedFile().toPath();
        try {
            loadedPlain = Files.readAllBytes(loadedPath);
            fileLabel.setText("Файл: " + loadedPath + " (" + loadedPlain.length + " байт)");
            plainBinaryArea.setText(formatFirstAndLastBytesGrouped(loadedPlain, PREVIEW_BYTES));
            cipherBinaryArea.setText("");
            keyBitsArea.setText("");
            lastOutput = null;
            statusLabel.setText("Файл загружен");
        } catch (IOException ex) {
            statusLabel.setText("Ошибка чтения: " + ex.getMessage());
        }
    }

    private void runCipher(boolean encrypt) {
        if (loadedPlain == null) {
            JOptionPane.showMessageDialog(this, "Сначала выберите файл.", "Нет данных", JOptionPane.WARNING_MESSAGE);
            return;
        }
        long init = Lfsr35.parseInitialState(registerField.getText());
        if (init == 0) {
            JOptionPane.showMessageDialog(this,
                    "Состояние не должно быть нулевым (нужна хотя бы одна «1» в " + Lfsr35.REGISTER_BIT_LENGTH + " битах).",
                    "Недопустимое состояние", JOptionPane.WARNING_MESSAGE);
            return;
        }

        Lfsr35 lfsr = new Lfsr35(init);
        byte[] in = loadedPlain;
        byte[] out = new byte[in.length];
        int n = in.length;
        int e = PREVIEW_BYTES;
        byte[] keyHead = new byte[Math.min(e, n)];
        byte[] keyTail = (n > e) ? new byte[e] : null;
        for (int i = 0; i < n; i++) {
            int kb = lfsr.nextKeyByte();
            out[i] = (byte) (in[i] ^ kb);
            if (i < keyHead.length) {
                keyHead[i] = (byte) kb;
            }
            if (keyTail != null && i >= n - e) {
                keyTail[i - (n - e)] = (byte) kb;
            }
        }
        lastOutput = out;
        keyBitsArea.setText(formatKeyStreamEdges(keyHead, keyTail, n, e));
        plainBinaryArea.setText(formatFirstAndLastBytesGrouped(in, e));
        cipherBinaryArea.setText(formatFirstAndLastBytesGrouped(out, e));
        statusLabel.setText(encrypt ? "Шифрование: XOR(M, K)." : "Расшифрование: XOR(C, K).");
    }

    private void saveResult() {
        if (lastOutput == null) {
            JOptionPane.showMessageDialog(this, "Нет результата для сохранения.", "Пусто", JOptionPane.WARNING_MESSAGE);
            return;
        }
        JFileChooser ch = new JFileChooser();
        if (loadedPath != null && loadedPath.getParent() != null) {
            ch.setCurrentDirectory(loadedPath.getParent().toFile());
            ch.setSelectedFile(loadedPath.getParent().resolve(loadedPath.getFileName().toString() + ".out").toFile());
        }
        if (ch.showSaveDialog(this) != JFileChooser.APPROVE_OPTION) {
            return;
        }
        Path target = ch.getSelectedFile().toPath();
        try {
            Files.write(target, lastOutput);
            statusLabel.setText("Сохранено: " + target);
        } catch (IOException ex) {
            statusLabel.setText("Ошибка записи: " + ex.getMessage());
        }
    }

    private void clearAll() {
        registerField.setText("");
        keyBitsArea.setText("");
        plainBinaryArea.setText("");
        cipherBinaryArea.setText("");
        loadedPlain = null;
        lastOutput = null;
        loadedPath = null;
        fileLabel.setText("Файл не выбран");
        statusLabel.setText("Поля очищены");
    }

    /**
     * Первые и последние {@code edge} байт файла; формат — см. {@link #formatByteRangeGrouped}.
     */
    private static String formatFirstAndLastBytesGrouped(byte[] data, int edge) {
        if (data == null || data.length == 0) {
            return "";
        }
        int n = data.length;
        if (n <= edge) {
            return "Весь файл (" + n + " байт):\n" + formatByteRangeGrouped(data, 0, n);
        }
        StringBuilder sb = new StringBuilder(n / 4 + 256);
        sb.append("Первые ").append(edge).append(" байт:\n");
        sb.append(formatByteRangeGrouped(data, 0, edge));
        if (n > 2 * edge) {
            sb.append("\n\n--- … (пропуск ").append(n - 2L * edge).append(" байт) … ---\n\n");
        } else {
            sb.append("\n\n");
        }
        sb.append("Последние ").append(edge).append(" байт:\n");
        sb.append(formatByteRangeGrouped(data, n - edge, edge));
        sb.append("\n\n(всего байт: ").append(n).append(")");
        if (n <= 2 * edge) {
            sb.append(" — блоки перекрываются");
        }
        return sb.toString();
    }

    /**
     * Первые {@code head} и последние {@code tail} байт гаммы (без хранения всего потока).
     */
    private static String formatKeyStreamEdges(byte[] head, byte[] tail, int n, int edge) {
        if (n == 0) {
            return "";
        }
        if (n <= edge) {
            return "Гамма K (" + n + " байт по длине файла):\n" + formatByteRangeGrouped(head, 0, n);
        }
        StringBuilder sb = new StringBuilder(n / 4 + 256);
        sb.append("Первые ").append(edge).append(" байт гаммы K:\n");
        sb.append(formatByteRangeGrouped(head, 0, edge));
        if (n > 2 * edge) {
            sb.append("\n\n--- … (пропуск ").append(n - 2L * edge).append(" байт) … ---\n\n");
        } else {
            sb.append("\n\n");
        }
        sb.append("Последние ").append(edge).append(" байт гаммы K:\n");
        sb.append(formatByteRangeGrouped(tail, 0, tail.length, n - edge));
        sb.append("\n\n(всего байт гаммы: ").append(n).append(")");
        if (n <= 2 * edge) {
            sb.append(" — блоки перекрываются");
        }
        return sb.toString();
    }

    /**
     * Участок {@code data[from .. from+len-1]}; подписи строк — те же индексы (срез из «целого» массива).
     */
    private static String formatByteRangeGrouped(byte[] data, int from, int len) {
        return formatByteRangeGrouped(data, from, len, from);
    }

    /**
     * Участок {@code data[arrayFrom .. arrayFrom+len-1]}; в подписи — глобальные индексы байтов,
     * где {@code data[arrayFrom]} соответствует индексу {@code globalIndexOfArrayFrom}.
     * Нужно для хвоста гаммы: в {@code tail} лежат байты с индексами файла {@code n-edge .. n-1}, но в массиве — с 0.
     */
    private static String formatByteRangeGrouped(byte[] data, int arrayFrom, int len, int globalIndexOfArrayFrom) {
        if (data == null || len <= 0 || arrayFrom < 0 || arrayFrom >= data.length) {
            return "";
        }
        len = Math.min(len, data.length - arrayFrom);
        int end = arrayFrom + len;
        StringBuilder sb = new StringBuilder(len * (8 + 2) + 64);
        int pos = arrayFrom;
        while (pos < end) {
            int lineEnd = Math.min(pos + BYTES_PER_LINE, end);
            int g0 = globalIndexOfArrayFrom + (pos - arrayFrom);
            int g1 = globalIndexOfArrayFrom + (lineEnd - 1 - arrayFrom);
            sb.append(String.format("[%02d–%02d]  ", g0, g1));
            for (int i = pos; i < lineEnd; i++) {
                if (i > pos) {
                    sb.append("  ");
                }
                sb.append(byteToBitString(data[i]));
            }
            sb.append('\n');
            pos = lineEnd;
        }
        return sb.toString();
    }

    private static String byteToBitString(byte value) {
        int v = value & 0xff;
        StringBuilder b = new StringBuilder(8);
        for (int bit = 7; bit >= 0; bit--) {
            b.append((v >>> bit) & 1);
        }
        return b.toString();
    }

    private static final class BinaryOnlyFilter extends DocumentFilter {
        @Override
        public void replace(FilterBypass fb, int offset, int length, String text, AttributeSet attrs)
                throws BadLocationException {
            if (text != null) {
                StringBuilder keep = new StringBuilder(text.length());
                for (int i = 0; i < text.length(); i++) {
                    char c = text.charAt(i);
                    if (c == '0' || c == '1') {
                        keep.append(c);
                    }
                }
                text = keep.toString();
            }
            super.replace(fb, offset, length, text, attrs);
        }

        @Override
        public void insertString(FilterBypass fb, int offset, String string, AttributeSet attr)
                throws BadLocationException {
            replace(fb, offset, 0, string, attr);
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            LfsrStreamCipherApp w = new LfsrStreamCipherApp();
            w.setVisible(true);
        });
    }
}
