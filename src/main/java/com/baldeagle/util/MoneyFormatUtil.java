package com.baldeagle.util;

import com.baldeagle.config.BaldeagleConfig;
import java.math.BigDecimal;
import java.math.RoundingMode;

public final class MoneyFormatUtil {

    private static final long THOUSAND = 1_000L;
    private static final long MILLION = 1_000_000L;
    private static final long BILLION = 1_000_000_000L;
    private static final long TRILLION = 1_000_000_000_000L;

    private MoneyFormatUtil() {}

    public static String format(long value) {
        return format(value, BaldeagleConfig.moneyFormatFullNumberThreshold);
    }

    public static String format(long value, long fullNumberThreshold) {
        BigDecimal absValue = BigDecimal.valueOf(value).abs();
        if (
            fullNumberThreshold > 0 &&
            absValue.compareTo(BigDecimal.valueOf(fullNumberThreshold)) < 0
        ) {
            return Long.toString(value);
        }

        if (absValue.compareTo(BigDecimal.valueOf(THOUSAND)) < 0) {
            return Long.toString(value);
        }

        long divisor = THOUSAND;
        String suffix = "K";
        if (absValue.compareTo(BigDecimal.valueOf(TRILLION)) >= 0) {
            divisor = TRILLION;
            suffix = "T";
        } else if (absValue.compareTo(BigDecimal.valueOf(BILLION)) >= 0) {
            divisor = BILLION;
            suffix = "B";
        } else if (absValue.compareTo(BigDecimal.valueOf(MILLION)) >= 0) {
            divisor = MILLION;
            suffix = "M";
        }

        BigDecimal scaled = absValue.divide(
            BigDecimal.valueOf(divisor),
            1,
            RoundingMode.HALF_UP
        );
        String formatted = scaled.stripTrailingZeros().toPlainString();
        if (value < 0) {
            formatted = "-" + formatted;
        }
        return formatted + suffix;
    }
}
