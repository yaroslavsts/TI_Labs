/**
 * LFSR по методичке: сдвиг разрядов влево на один; в младший (35-й) разряд
 * вдвигается XOR разрядов, отмеченных ненулевыми коэффициентами P(x) (кроме ведущего x^35);
 * бит ключевого потока Ki — значение старшего разряда b₁ до такта.
 * <p>
 * Примитивный многочлен: P(x) = x^35 + x^2 + 1.
 * Участвуют в обратной связи старший разряд и разряд, соответствующий x^2
 * (вторая «ячейка» от старшего — b₁ ⊕ b₃₄ в записи MSB…LSB для 35 бит).
 * Период M-последовательности: 2^35 − 1.
 */
public final class Lfsr35 {
    /** Длина начального состояния I (число разрядов регистра). */
    public static final int REGISTER_BIT_LENGTH = 35;
    private static final int DEGREE = REGISTER_BIT_LENGTH;
    private static final long MASK = (1L << DEGREE) - 1;

    /** Состояние: бит {@code DEGREE-1} — старший (b₁), бит 0 — младший (b₃₅). */
    private long state;

    public Lfsr35(long initialState) {
        this.state = initialState & MASK;
        if (this.state == 0) {
            throw new IllegalArgumentException("Начальное состояние LFSR не должно быть нулевым");
        }
    }

    /**
     * Один такт: Ki = старший бит; затем сдвиг влево, справа (LSB) вдвигается обратная связь.
     */
    private int nextBit() {
        long msb = (state >>> (DEGREE - 1)) & 1L;
        long tapX2 = (state >>> 1) & 1L;
        long fb = msb ^ tapX2;
        int keyBit = (int) msb;
        state = ((state << 1) | fb) & MASK;
        return keyBit;
    }

    /** Байт гаммы: первый сгенерированный бит — старший бит байта. */
    public int nextKeyByte() {
        int b = 0;
        for (int i = 7; i >= 0; i--) {
            b |= nextBit() << i;
        }
        return b;
    }

    /**
     * Строка из '0'/'1': слева направо — от старшего разряда к младшему (как на схеме).
     * Берутся первые 35 символов; при нехватке дополняются нулями слева (старшие разряды).
     */
    public static long parseInitialState(String onesAndZeros) {
        if (onesAndZeros == null) {
            return 0;
        }
        String bits = onesAndZeros;
        if (bits.length() > DEGREE) {
            bits = bits.substring(0, DEGREE);
        }
        while (bits.length() < DEGREE) {
            bits = '0' + bits;
        }
        long v = 0;
        for (int i = 0; i < DEGREE; i++) {
            v <<= 1;
            if (bits.charAt(i) == '1') {
                v |= 1;
            }
        }
        return v;
    }

    public static String polynomialDescription() {
        return "P(x) = x^35 + x^2 + 1";
    }
}
