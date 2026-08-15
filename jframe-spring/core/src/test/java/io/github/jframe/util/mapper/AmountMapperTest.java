package io.github.jframe.util.mapper;

import io.github.support.UnitTest;

import java.math.BigDecimal;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

/**
 * Unit tests for {@link AmountMapper}.
 *
 * <p>Verifies that {@link AmountMapper#normalizeAmount(BigDecimal)} delegates faithfully to
 * {@code AmountUtils.normalizeAmount} — a representative subset of the AmountUtils contract.
 */
@DisplayName("Utility - AmountMapper")
class AmountMapperTest extends UnitTest {

    private AmountMapper amountMapper;

    @Override
    @BeforeEach
    public void setUp() {
        super.setUp();
        amountMapper = new AmountMapper();
    }

    @Test
    @DisplayName("Should return null when input is null (delegates to AmountUtils)")
    void shouldReturnNullWhenInputIsNull() {
        // Given: A null amount
        final BigDecimal input = null;

        // When: Normalising via the mapper
        final BigDecimal result = amountMapper.normalizeAmount(input);

        // Then: Null is returned (AmountUtils null-safe contract honoured)
        assertThat(result, is(nullValue()));
    }

    @Test
    @DisplayName("Should set scale to 2 for whole number (10 → 10.00)")
    void shouldNormalizeWholeNumberToScaleTwo() {
        // Given: A whole number
        final BigDecimal input = new BigDecimal("10");

        // When: Normalising via the mapper
        final BigDecimal result = amountMapper.normalizeAmount(input);

        // Then: Scale is 2 (minimum floor applied)
        assertThat(result, is(notNullValue()));
        assertThat(result.scale(), is(2));
        assertThat(result.toPlainString(), is("10.00"));
    }

    @Test
    @DisplayName("CONTRACT-DEFINING: Should preserve greater scale (1.2345 → 1.2345)")
    void shouldPreserveGreaterScaleViaDelegation() {
        // Given: A BigDecimal with 4 decimal places
        final BigDecimal input = new BigDecimal("1.2345");

        // When: Normalising via the mapper
        final BigDecimal result = amountMapper.normalizeAmount(input);

        // Then: Scale 4 preserved — mapper does not add rounding beyond AmountUtils contract
        assertThat(result, is(notNullValue()));
        assertThat(result.scale(), is(4));
        assertThat(result.toPlainString(), is("1.2345"));
    }

    @Test
    @DisplayName("Should strip trailing zeros then apply floor (1.500 → 1.50)")
    void shouldStripTrailingZerosThenApplyFloor() {
        // Given: A value with redundant trailing zeros
        final BigDecimal input = new BigDecimal("1.500");

        // When: Normalising via the mapper
        final BigDecimal result = amountMapper.normalizeAmount(input);

        // Then: Scale drops to 2 (trailing zero stripped, floor applied)
        assertThat(result, is(notNullValue()));
        assertThat(result.scale(), is(2));
        assertThat(result.toPlainString(), is("1.50"));
    }

    @Test
    @DisplayName("Should normalise negative values the same as AmountUtils (-5 → -5.00)")
    void shouldNormalizeNegativeValue() {
        // Given: A negative whole number
        final BigDecimal input = new BigDecimal("-5");

        // When: Normalising via the mapper
        final BigDecimal result = amountMapper.normalizeAmount(input);

        // Then: Scale is 2
        assertThat(result, is(notNullValue()));
        assertThat(result.scale(), is(2));
        assertThat(result.toPlainString(), is("-5.00"));
    }
}
