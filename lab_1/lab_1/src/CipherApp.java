import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Вариант 11: Шифрование/дешифрование текста.
 * - Шифр Плейфера с 4 таблицами (английский)
 * - Шифр Виженера с прямым ключом (русский)
 */
public class CipherApp extends JFrame {
    private final PlayfairCipher playfair = new PlayfairCipher();
    private final VigenereCipher vigenere = new VigenereCipher();

    private JTextArea inputArea;
    private JTextArea outputArea;
    private JComboBox<String> algorithmCombo;
    private JTextField vigenereKeyField;
    private JTextField playfairKey1, playfairKey2, playfairKey3, playfairKey4;
    private JPanel keysCardPanel;
    private JLabel statusLabel;

    public CipherApp() {
        setTitle("Шифрование текста");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(800, 650);
        setLocationRelativeTo(null);

        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception ignored) {}

        Font font14 = new Font("Dialog", Font.PLAIN, 14);

        JPanel main = new JPanel(new BorderLayout(10, 10));
        main.setBorder(new EmptyBorder(15, 15, 15, 15));

        JPanel northPanel = new JPanel(new BorderLayout(0, 5));
        JPanel top = new JPanel(new FlowLayout(FlowLayout.LEFT));
        top.add(new JLabel("Алгоритм:"));
        algorithmCombo = new JComboBox<>(new String[]{"Шифр Плейфера (англ.)", "Шифр Виженера (рус.)"});
        algorithmCombo.setFont(font14);
        algorithmCombo.addActionListener(e -> switchKeyPanels());
        top.add(algorithmCombo);
        northPanel.add(top, BorderLayout.NORTH);

        keysCardPanel = new JPanel(new CardLayout());
        keysCardPanel.add(createVigenereKeyPanel(font14), "vigenere");
        keysCardPanel.add(createPlayfairKeysPanel(font14), "playfair");
        northPanel.add(keysCardPanel, BorderLayout.CENTER);
        main.add(northPanel, BorderLayout.NORTH);

        JPanel center = new JPanel(new BorderLayout(0, 5));
        inputArea = new JTextArea(6, 40);
        inputArea.setFont(font14);
        inputArea.setLineWrap(true);
        inputArea.setWrapStyleWord(true);
        JScrollPane inputScroll = new JScrollPane(inputArea);
        inputScroll.setBorder(BorderFactory.createTitledBorder("Исходный текст"));
        inputScroll.setPreferredSize(new Dimension(0, 180));

        outputArea = new JTextArea(6, 40);
        outputArea.setFont(font14);
        outputArea.setEditable(false);
        outputArea.setLineWrap(true);
        outputArea.setWrapStyleWord(true);
        JScrollPane outputScroll = new JScrollPane(outputArea);
        outputScroll.setBorder(BorderFactory.createTitledBorder("Результат"));
        outputScroll.setPreferredSize(new Dimension(0, 180));

        center.add(inputScroll, BorderLayout.NORTH);
        center.add(outputScroll, BorderLayout.CENTER);
        main.add(center, BorderLayout.CENTER);

        JPanel bottom = new JPanel(new BorderLayout(0, 8));
        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 5));
        JButton loadBtn = new JButton("Прочитать из файла");
        JButton saveBtn = new JButton("Сохранить в файл");
        JButton encryptBtn = new JButton("Шифровать");
        JButton decryptBtn = new JButton("Дешифровать");
        JButton clearBtn = new JButton("Очистить всё");
        for (JButton b : new JButton[]{loadBtn, saveBtn, encryptBtn, decryptBtn, clearBtn}) {
            b.setFont(font14);
            buttons.add(b);
        }
        loadBtn.addActionListener(e -> loadFromFile());
        saveBtn.addActionListener(e -> saveToFile());
        encryptBtn.addActionListener(e -> encrypt());
        decryptBtn.addActionListener(e -> decrypt());
        clearBtn.addActionListener(e -> clearAll());
        bottom.add(buttons, BorderLayout.CENTER);

        statusLabel = new JLabel(" ");
        statusLabel.setFont(font14);
        bottom.add(statusLabel, BorderLayout.SOUTH);
        main.add(bottom, BorderLayout.SOUTH);

        setContentPane(main);
        switchKeyPanels();
    }

    private JPanel createVigenereKeyPanel(Font font) {
        JPanel p = new JPanel(new FlowLayout(FlowLayout.LEFT));
        p.add(new JLabel("Ключ (рус.):"));
        vigenereKeyField = new JTextField(30);
        vigenereKeyField.setFont(font);
        vigenereKeyField.setText("КЛЮЧ");
        p.add(vigenereKeyField);
        return p;
    }

    private JPanel createPlayfairKeysPanel(Font font) {
        JPanel p = new JPanel(new GridLayout(4, 2, 5, 5));
        p.setBorder(BorderFactory.createTitledBorder("4 ключа (англ.)"));
        playfairKey1 = new JTextField(20);
        playfairKey2 = new JTextField(20);
        playfairKey3 = new JTextField(20);
        playfairKey4 = new JTextField(20);
        playfairKey1.setText("CRYPTO");
        playfairKey2.setText("GRAPHY");
        playfairKey3.setText("SECURE");
        playfairKey4.setText("MESSAGE");
        for (JTextField f : new JTextField[]{playfairKey1, playfairKey2, playfairKey3, playfairKey4}) {
            f.setFont(font);
        }
        p.add(new JLabel("Ключ 1:"));
        p.add(playfairKey1);
        p.add(new JLabel("Ключ 2:"));
        p.add(playfairKey2);
        p.add(new JLabel("Ключ 3:"));
        p.add(playfairKey3);
        p.add(new JLabel("Ключ 4:"));
        p.add(playfairKey4);
        return p;
    }

    private void switchKeyPanels() {
        CardLayout cl = (CardLayout) keysCardPanel.getLayout();
        cl.show(keysCardPanel, algorithmCombo.getSelectedIndex() == 0 ? "playfair" : "vigenere");
    }

    private void encrypt() {
        String text = inputArea.getText();
        if (text.trim().isEmpty()) {
            showStatus("Введите текст для шифрования!", true);
            return;
        }
        try {
            String result;
            if (algorithmCombo.getSelectedIndex() == 0) {
                result = playfair.encrypt(text, playfairKey1.getText(), playfairKey2.getText(),
                        playfairKey3.getText(), playfairKey4.getText());
            } else {
                result = vigenere.encrypt(text, vigenereKeyField.getText());
            }
            outputArea.setText(result);
            outputArea.setCaretPosition(0);
            outputArea.revalidate();
            showStatus(result.isEmpty() ? "Нет букв для шифрования (для Плейфера — английский, для Виженера — русский)" : "Текст успешно зашифрован!");
        } catch (Exception ex) {
            showStatus("Ошибка: " + ex.getMessage(), true);
        }
    }

    private void decrypt() {
        String text = inputArea.getText();
        if (text.trim().isEmpty()) {
            showStatus("Введите текст для расшифрования!", true);
            return;
        }
        try {
            String result;
            if (algorithmCombo.getSelectedIndex() == 0) {
                result = playfair.decrypt(text, playfairKey1.getText(), playfairKey2.getText(),
                        playfairKey3.getText(), playfairKey4.getText());
            } else {
                result = vigenere.decrypt(text, vigenereKeyField.getText());
            }
            outputArea.setText(result);
            outputArea.setCaretPosition(0);
            outputArea.revalidate();
            showStatus(result.isEmpty() ? "Нет букв для расшифрования" : "Текст успешно расшифрован!");
        } catch (Exception ex) {
            showStatus("Ошибка: " + ex.getMessage(), true);
        }
    }

    private void loadFromFile() {
        JFileChooser fc = new JFileChooser();
        if (fc.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
            try {
                String content = new String(Files.readAllBytes(fc.getSelectedFile().toPath()), StandardCharsets.UTF_8);
                inputArea.setText(content);
                showStatus("Файл загружен: " + fc.getSelectedFile().getName());
            } catch (Exception ex) {
                showStatus("Ошибка чтения: " + ex.getMessage(), true);
            }
        }
    }

    private void saveToFile() {
        String text = outputArea.getText();
        if (text.isEmpty()) {
            showStatus("Нет данных для сохранения!", true);
            return;
        }
        JFileChooser fc = new JFileChooser();
        if (fc.showSaveDialog(this) == JFileChooser.APPROVE_OPTION) {
            try {
                Path path = fc.getSelectedFile().toPath();
                Files.write(path, text.getBytes(StandardCharsets.UTF_8));
                showStatus("Файл сохранён: " + fc.getSelectedFile().getName());
            } catch (Exception ex) {
                showStatus("Ошибка сохранения: " + ex.getMessage(), true);
            }
        }
    }

    private void clearAll() {
        inputArea.setText("");
        outputArea.setText("");
        vigenereKeyField.setText("КЛЮЧ");
        playfairKey1.setText("CRYPTO");
        playfairKey2.setText("GRAPHY");
        playfairKey3.setText("SECURE");
        playfairKey4.setText("MESSAGE");
        showStatus("Поля очищены");
    }

    private void showStatus(String msg, boolean error) {
        statusLabel.setText(msg);
        statusLabel.setForeground(error ? Color.RED : Color.BLACK);
    }

    private void showStatus(String msg) {
        showStatus(msg, false);
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            CipherApp app = new CipherApp();
            app.setVisible(true);
        });
    }
}
