package io.github.jframe.util.mapper;

import io.github.jframe.util.AmountUtils;

import java.math.BigDecimal;

import org.mapstruct.Mapper;
import org.mapstruct.Named;

/**
 * MapStruct mapper for {@link BigDecimal} amount normalisation.
 *
 * <p>Registered in {@link io.github.jframe.util.mapper.config.SharedMapperConfig} via
 * {@code uses}. The method is annotated with {@code @Named} so MapStruct does NOT auto-select it
 * for same-type {@code BigDecimal → BigDecimal} assignments — consumers must explicitly qualify a
 * mapping with {@code qualifiedByName = "normalizeAmount"} to trigger normalisation.
 *
 * <p>This class is intentionally non-abstract so that it can be instantiated directly in unit
 * tests without an annotation-processor-generated subclass.
 *
 * <p>Uses {@code componentModel = "spring"} directly (not via {@link
 * io.github.jframe.util.mapper.config.SharedMapperConfig}) to avoid a MapStruct compile-time
 * warning about self-reference in the {@code uses} registry.
 */
@Mapper(componentModel = "spring")
public class AmountMapper {

    /**
     * Normalises a monetary amount to a minimum scale of 2, preserving any greater existing scale.
     *
     * <p>Delegates to {@link AmountUtils#normalizeAmount(BigDecimal)}. See that method for the
     * full contract.
     *
     * @param value the amount to normalise, may be {@code null}
     * @return the normalised amount, or {@code null} if the input was {@code null}
     */
    @Named("normalizeAmount")
    public BigDecimal normalizeAmount(final BigDecimal value) {
        return AmountUtils.normalizeAmount(value);
    }
}
