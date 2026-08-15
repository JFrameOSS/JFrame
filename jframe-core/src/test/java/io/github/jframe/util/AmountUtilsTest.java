package io.github.jframe.util;

import io.github.support.UnitTest;

import java.math.BigDecimal;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Unit tests for {@link AmountUtils}.
 *
 * <p>Verifies the normalizeAmount contract:
 * <ul>
 * <li>null in → null out</li>
 * <li>trailing zeros stripped, then minimum scale of 2 enforced</li>
 * <li>scale already ≥ 2 is preserved unchanged</li>
 * </ul>
 */
@DisplayName("Utility - AmountUtils")
class AmountUtilsTest extends UnitTest {

    // -------------------------------------------------------------------------
    // normalizeAmount — happy-path scale enforcement
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("Should set scale to 2 when value is a whole number (10 → 10.00)")
    void shouldNormalizeWholeNumberToScaleTwo() {
        // Given: A whole number BigDecimal
        final BigDecimal input = new BigDecimal("10");

        // When: Normalising
        final BigDecimal result = AmountUtils.normalizeAmount(input);

        // Then: Scale is exactly 2
        assertThat(result, is(notNullValue()));
        assertThat(result.scale(), is(2));
        assertThat(result.toPlainString(), is("10.00"));
    }

    @Test
    @DisplayName("Should pad to scale 2 when value has one decimal place (10.5 → 10.50)")
    void shouldNormalizeOneDecimalToScaleTwo() {
        // Given: A BigDecimal with one decimal place
        final BigDecimal input = new BigDecimal("10.5");

        // When: Normalising
        final BigDecimal result = AmountUtils.normalizeAmount(input);

        // Then: Scale is exactly 2
        assertThat(result, is(notNullValue()));
        assertThat(result.scale(), is(2));
        assertThat(result.toPlainString(), is("10.50"));
    }

    @Test
    @DisplayName("Should leave value unchanged when scale is already 2 (10.00 → 10.00)")
    void shouldPreserveValueWithScaleAlreadyTwo() {
        // Given: A BigDecimal already at scale 2
        final BigDecimal input = new BigDecimal("10.00");

        // When: Normalising
        final BigDecimal result = AmountUtils.normalizeAmount(input);

        // Then: Scale remains 2
        assertThat(result, is(notNullValue()));
        assertThat(result.scale(), is(2));
        assertThat(result.toPlainString(), is("10.00"));
    }

    @Test
    @DisplayName("CONTRACT-DEFINING: Should preserve greater scale (1.2345 → 1.2345, NOT 1.23)")
    void shouldPreserveGreaterScaleAndNotTruncate() {
        // Given: A BigDecimal with 4 decimal places — this proves the contract is
        //        "minimum scale 2, greater scale preserved", NOT "always round to 2".
        final BigDecimal input = new BigDecimal("1.2345");

        // When: Normalising
        final BigDecimal result = AmountUtils.normalizeAmount(input);

        // Then: Scale is 4 — unchanged; no rounding occurs
        assertThat(result, is(notNullValue()));
        assertThat(result.scale(), is(4));
        assertThat(result.toPlainString(), is("1.2345"));
    }

    @Test
    @DisplayName("Should set scale to 2 when value is zero (0 → 0.00)")
    void shouldNormalizeZeroToScaleTwo() {
        // Given: Zero with no decimal places
        final BigDecimal input = new BigDecimal("0");

        // When: Normalising
        final BigDecimal result = AmountUtils.normalizeAmount(input);

        // Then: Scale is 2
        assertThat(result, is(notNullValue()));
        assertThat(result.scale(), is(2));
        assertThat(result.toPlainString(), is("0.00"));
    }

    @Test
    @DisplayName("Should normalise negative values the same way (-5 → -5.00)")
    void shouldNormalizeNegativeValueToScaleTwo() {
        // Given: A negative whole number
        final BigDecimal input = new BigDecimal("-5");

        // When: Normalising
        final BigDecimal result = AmountUtils.normalizeAmount(input);

        // Then: Scale is 2
        assertThat(result, is(notNullValue()));
        assertThat(result.scale(), is(2));
        assertThat(result.toPlainString(), is("-5.00"));
    }

    @Test
    @DisplayName("Should strip trailing zeros before applying floor (1.500 → 1.50, not 1.500)")
    void shouldStripTrailingZerosBeforeApplyingFloor() {
        // Given: A BigDecimal with redundant trailing zeros
        final BigDecimal input = new BigDecimal("1.500");

        // When: Normalising
        final BigDecimal result = AmountUtils.normalizeAmount(input);

        // Then: Trailing zeros stripped to the minimum-2 floor — scale is 2
        assertThat(result, is(notNullValue()));
        assertThat(result.scale(), is(2));
        assertThat(result.toPlainString(), is("1.50"));
    }

    // -------------------------------------------------------------------------
    // normalizeAmount — null safety
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("Should return null when input is null")
    void shouldReturnNullWhenInputIsNull() {
        // Given: A null input
        final BigDecimal input = null;

        // When: Normalising
        final BigDecimal result = AmountUtils.normalizeAmount(input);

        // Then: Null is returned without exception
        assertThat(result, is(nullValue()));
    }

    // -------------------------------------------------------------------------
    // normalizeAmount — boundary / extreme values
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("Should handle very large scale (e.g. 1.000000000001) without throwing")
    void shouldHandleVeryLargeScaleWithoutThrowing() {
        // Given: A value with many decimal places
        final BigDecimal input = new BigDecimal("1.000000000001");

        // When: Normalising
        final BigDecimal result = AmountUtils.normalizeAmount(input);

        // Then: No exception; scale is preserved (12 in this case)
        assertThat(result, is(notNullValue()));
        assertThat(result.scale(), is(greaterThanOrEqualTo(2)));
        assertThat(result.toPlainString(), is("1.000000000001"));
    }

    @Test
    @DisplayName("Should handle very large magnitude value without throwing")
    void shouldHandleVeryLargeMagnitudeWithoutThrowing() {
        // Given: A very large magnitude with 2 decimal places
        final BigDecimal input = new BigDecimal("99999999999999999.99");

        // When: Normalising
        final BigDecimal result = AmountUtils.normalizeAmount(input);

        // Then: No exception; scale remains 2
        assertThat(result, is(notNullValue()));
        assertThat(result.scale(), is(2));
    }

    // -------------------------------------------------------------------------
    // Non-instantiability
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("Should not be instantiable (private constructor)")
    void shouldNotBeInstantiable() {
        // Given: The AmountUtils class has a private constructor
        // When: Attempting to invoke it via reflection
        // Then: An exception is thrown — the class is a pure static utility
        assertThrows(Exception.class, () -> {
            final java.lang.reflect.Constructor<AmountUtils> ctor =
                AmountUtils.class.getDeclaredConstructor();
            ctor.setAccessible(true);
            ctor.newInstance();
        });
    }
}
