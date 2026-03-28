import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Вывод примера работы LFSR по тактам для отчёта (не менее 60 тактов).
 * Запуск: {@code java -cp out LfsrExampleTrace [начальные_35_бит_0_1]}
 * <p>
 * Без аргумента используется ненулевое состояние по умолчанию.
 */
public final class LfsrExampleTrace {
    private static final int TACTS = 60;

    public static void main(String[] args) throws IOException {
        String seed = args.length > 0 ? args[0] : "10000000000000000000000000000000001";
        long init = Lfsr35.parseInitialState(seed);
        if (init == 0) {
            System.err.println("Нужно ненулевое начальное состояние (35 бит).");
            System.exit(1);
        }
        Lfsr35 lfsr = new Lfsr35(init);

        StringBuilder table = new StringBuilder(8000);
        table.append("Полином: ").append(Lfsr35.polynomialDescription()).append('\n');
        table.append("Начальное I (MSB…LSB): ").append(lfsr.stateAsBitString()).append('\n');
        table.append("Такт\tСостояние до сдвига (b₁…b₃₅)\tK_i\n");

        for (int t = 1; t <= TACTS; t++) {
            table.append(Lfsr35.formatTactLine(t, lfsr)).append('\n');
        }

        System.out.print(table);

        Path out = Path.of("lfsr35_tacts_60.txt");
        Files.writeString(out, table.toString(), StandardCharsets.UTF_8);
        System.err.println("Таблица также записана в файл: " + out.toAbsolutePath());
    }
}
