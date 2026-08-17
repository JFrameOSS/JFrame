package io.github.jframe.util;

import java.math.BigDecimal;
import java.math.RoundingMode;

import static java.util.Objects.isNull;

/**
 * Utility class for monetary and numeric amount normalisation.
 *
 * <p>This is not a display formatter — it operates on {@link BigDecimal} values
 * and is intended for canonical in-memory representation before persistence or
 * comparison.
 *
 * <p>Contract: Normalises to a MINIMUM scale of 2; a greater existing scale is
 * preserved, never rounded.
 * <ul>
 * <li>{@code 10} → {@code 10.00}</li>
 * <li>{@code 1.500} → {@code 1.50}</li>
 * <li>{@code 1.2345} → {@code 1.2345}</li>
 * </ul>
 */
public final class AmountUtils {

    private AmountUtils() {
        throw new UnsupportedOperationException("Utility class");
    }

    /**
     * Normalises the given amount to a minimum scale of 2.
     *
     * <p>Trailing zeros are stripped first; then, if the resulting scale is less than
     * 2, the scale is widened to 2 using {@link RoundingMode#UNNECESSARY}. The
     * {@code UNNECESSARY} mode is unreachable here (widening never rounds) but makes
     * the invariant self-documenting and turns any future violation into a loud
     * {@link ArithmeticException} instead of silent rounding.
     *
     * @param value the amount to normalise, may be {@code null}
     * @return the normalised amount, or {@code null} if the input was {@code null}
     */
    public static BigDecimal normalizeAmount(final BigDecimal value) {
        if (isNull(value)) {
            return null;
        }
        final BigDecimal stripped = value.stripTrailingZeros();
        return stripped.scale() < 2 ? stripped.setScale(2, RoundingMode.UNNECESSARY) : stripped;
    }
}
