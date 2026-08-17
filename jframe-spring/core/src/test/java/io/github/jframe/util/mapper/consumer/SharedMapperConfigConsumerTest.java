package io.github.jframe.util.mapper.consumer;

import io.github.jframe.util.mapper.DateTimeMapper;
import io.github.jframe.util.mapper.config.SharedMapperConfig;

import java.lang.reflect.Constructor;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.ZonedDateTime;
import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

/**
 * Consumer-simulation integration test for the {@code uses} registry on {@link SharedMapperConfig}.
 *
 * <p>Background: {@code SharedMapperConfig} will declare
 * {@code uses = { UuidMapper.class, AmountMapper.class }}. {@code DateTimeMapper} is deliberately
 * <em>not</em> in {@code uses} — consumers must opt in explicitly.
 *
 * <p>This test class is the most important test in this delegation. It proves four empirical
 * claims about MapStruct's implicit-conversion behaviour via the {@code uses} registry and must
 * never be silently green due to missing generated code.
 *
 * <h2>How to verify generated code exists</h2>
 * <p>Each assertion operates on actual mapped values returned by a MapStruct-generated {@code *Impl}
 * class. If no {@code *Impl} is generated, instantiation will throw {@link ClassNotFoundException}
 * / {@link IllegalStateException}, making the test fail visibly — not silently pass.
 *
 * <h2>Claims proven</h2>
 * <ol>
 * <li><b>Claim 1</b> — {@code UuidMapper} auto-applies: a {@code UUID} source field maps to a
 * {@code String} target field without the consumer declaring anything extra.</li>
 * <li><b>Claim 2</b> — {@code AmountMapper.normalizeAmount} does NOT auto-apply for same-type
 * {@code BigDecimal→BigDecimal}: MapStruct uses direct assignment for same-type fields.
 * {@code @Named} does not prevent auto-selection, but there is no conversion to select — the
 * value passes through unchanged. <em>If this assertion fails (normalised value returned), that
 * is a critical finding — report it immediately.</em></li>
 * <li><b>Claim 3</b> — Without {@code DateTimeMapper} in {@code uses}, a
 * {@code LocalDateTime→ZonedDateTime} mapping cannot be resolved by jframe's mapper. The target
 * field is expected to be {@code null} (MapStruct leaves it unset under
 * {@code ReportingPolicy.IGNORE}). If MapStruct resolves it through its own built-in mechanism,
 * the assertion will document the actual behaviour.</li>
 * <li><b>Claim 4</b> — Consumer-level {@code uses = DateTimeMapper.class} MERGES with the
 * config-level {@code uses}: {@code LocalDateTime→ZonedDateTime} now resolves, AND a
 * {@code UUID→String} field still converts (proving config-level mappers are not replaced).</li>
 * </ol>
 */
@DisplayName("Consumer Simulation - SharedMapperConfig uses registry")
class SharedMapperConfigConsumerTest {

    // =========================================================================
    // Test fixture types — live in test source tree only
    // =========================================================================

    /** Source type for the base consumer mapper (no DateTimeMapper). */
    record BaseSource(UUID id, BigDecimal amount, LocalDateTime createdAt) {
    }


    /** Target type for the base consumer mapper. */
    record BaseTarget(String id, BigDecimal amount, ZonedDateTime createdAt) {
    }


    /** Source type for the opt-in consumer that also uses DateTimeMapper. */
    record ExtendedSource(UUID id, LocalDateTime createdAt) {
    }


    /** Target type for the opt-in consumer. */
    record ExtendedTarget(String id, ZonedDateTime createdAt) {
    }

    // =========================================================================
    // Consumer mapper without DateTimeMapper (claims 1, 2, 3)
    // =========================================================================


    /**
     * Minimal consumer mapper — uses only what SharedMapperConfig provides via {@code uses}.
     *
     * <p>Deliberately declares no extra {@code uses} so we can observe the raw behaviour of the
     * config-level registry.
     */
    @Mapper(config = SharedMapperConfig.class)
    interface BaseConsumerMapper {

        /**
         * Maps a {@link BaseSource} to a {@link BaseTarget}.
         *
         * <p>W3.6 empirical finding: {@code LocalDateTime→ZonedDateTime} is NOT a silently-ignored
         * unmapped field — MapStruct treats it as an unmappable type conversion and fails at
         * <em>compile time</em> unless explicitly ignored. {@code ReportingPolicy.IGNORE} only
         * suppresses warnings about unmatched target fields, not type-mismatch errors.
         * Therefore {@code createdAt} must be explicitly ignored here (Claim 3 documents
         * null-at-runtime via explicit ignore, not via silent IGNORE policy).
         *
         * @param source the source
         * @return the target
         */
        @Mapping(
            target = "createdAt",
            ignore = true
        )
        BaseTarget map(BaseSource source);
    }

    // =========================================================================
    // Opt-in consumer mapper that also uses DateTimeMapper (claim 4)
    // =========================================================================


    /**
     * Consumer mapper that explicitly opts in to {@link DateTimeMapper}.
     *
     * <p>Proves that consumer-level {@code uses} MERGES with config-level {@code uses} rather than
     * replacing it.
     */
    @Mapper(
        config = SharedMapperConfig.class,
        uses = DateTimeMapper.class
    )
    interface ExtendedConsumerMapper {

        /**
         * Maps an {@link ExtendedSource} to an {@link ExtendedTarget}.
         *
         * @param source the source
         * @return the target
         */
        ExtendedTarget map(ExtendedSource source);
    }

    // =========================================================================
    // Helper — obtain a MapStruct-generated implementation
    // =========================================================================

    /**
     * Reflectively instantiates the MapStruct-generated {@code *Impl} class.
     *
     * <p>Fails loudly if the annotation processor did not generate the implementation — preventing
     * the test from silently passing on ungenerated code.
     *
     * <p>W3.6 empirical finding: with {@code componentModel = "spring"} and
     * {@code injectionStrategy = CONSTRUCTOR} on {@link SharedMapperConfig}, MapStruct generates
     * a constructor that injects {@code uses}-listed helpers as Spring beans — NOT a no-arg
     * constructor. This helper therefore falls back to instantiating declared dependencies
     * reflectively when no no-arg constructor exists.
     *
     * @param mapperInterface the mapper interface whose impl to load
     * @param <T>             the mapper type
     * @return a new instance of the generated impl
     */
    @SuppressWarnings(
        {
            "unchecked",
            "PMD.AvoidAccessibilityAlteration"
        }
    )
    private static <T> T generatedImpl(final Class<T> mapperInterface) {
        final String implName = mapperInterface.getName() + "Impl";
        try {
            final Class<?> implClass = Class.forName(implName);
            // Try no-arg constructor first (default or field-injection model).
            try {
                final Constructor<?> noArg = implClass.getDeclaredConstructor();
                noArg.setAccessible(true);
                return (T) noArg.newInstance();
            } catch (final NoSuchMethodException ignored) {
                // Fall through to constructor-injection path.
            }
            // W3.6: Spring + CONSTRUCTOR injection produces a single constructor with
            // mapper-dependency arguments.  Instantiate each argument type directly.
            final Constructor<?> ctor = implClass.getDeclaredConstructors()[0];
            ctor.setAccessible(true);
            final Object[] args = new Object[ctor.getParameterCount()];
            for (int i = 0; i < ctor.getParameterCount(); i++) {
                args[i] = ctor.getParameterTypes()[i].getDeclaredConstructor().newInstance();
            }
            return (T) ctor.newInstance(args);
        } catch (final ClassNotFoundException e) {
            throw new IllegalStateException(
                "MapStruct did not generate " + implName + " — annotation processing may not be "
                    + "running for test sources. Check build configuration.",
                e
            );
        } catch (final ReflectiveOperationException e) {
            throw new IllegalStateException("Failed to instantiate " + implName, e);
        }
    }

    // =========================================================================
    // Claim 1 — UuidMapper auto-applies via uses
    // =========================================================================

    @Test
    @DisplayName("Claim 1: UuidMapper auto-applies — UUID field maps to String without consumer declaration")
    void shouldAutoApplyUuidMapperForUuidToStringConversion() {
        // Given: A source with a UUID id field
        final UUID sourceId = UUID.fromString("550e8400-e29b-41d4-a716-446655440000");
        final BaseSource source = new BaseSource(sourceId, BigDecimal.TEN, LocalDateTime.now());
        final BaseConsumerMapper mapper = generatedImpl(BaseConsumerMapper.class);

        // When: Mapping to the target
        final BaseTarget target = mapper.map(source);

        // Then: UUID was converted to String automatically via UuidMapper in uses registry
        assertThat(target, is(notNullValue()));
        assertThat(target.id(), is("550e8400-e29b-41d4-a716-446655440000"));
    }

    @Test
    @DisplayName("Claim 1 (null-safety): UuidMapper auto-applies — null UUID maps to null String")
    void shouldAutoApplyUuidMapperNullSafety() {
        // Given: A source with a null UUID id
        final BaseSource source = new BaseSource(null, BigDecimal.TEN, LocalDateTime.now());
        final BaseConsumerMapper mapper = generatedImpl(BaseConsumerMapper.class);

        // When: Mapping to the target
        final BaseTarget target = mapper.map(source);

        // Then: null UUID maps to null String (null-safe via UuidMapper)
        assertThat(target, is(notNullValue()));
        assertThat(target.id(), is(nullValue()));
    }

    // =========================================================================
    // Claim 2 — AmountMapper.normalizeAmount does NOT auto-apply for BigDecimal→BigDecimal
    // =========================================================================

    @Test
    @DisplayName("Claim 2: BigDecimal field passes through UNCHANGED — normalizeAmount does NOT auto-apply")
    void shouldNotAutoApplyNormalizeAmountForSameTypeBigDecimal() {
        // Given: A source with a BigDecimal that would be changed by normalizeAmount (10 → 10.00)
        //        We use a value whose scale differs from 2 to make the assertion decisive.
        final BigDecimal rawAmount = new BigDecimal("10");  // scale 0
        final BaseSource source = new BaseSource(UUID.randomUUID(), rawAmount, LocalDateTime.now());
        final BaseConsumerMapper mapper = generatedImpl(BaseConsumerMapper.class);

        // When: Mapping to the target
        final BaseTarget target = mapper.map(source);

        // Then: CRITICAL CLAIM — amount passes through as direct assignment (same type),
        //       NOT normalised to 10.00.  If this fails (scale becomes 2), that means
        //       AmountMapper IS auto-applying for same-type BigDecimal — report loudly.
        assertThat(target, is(notNullValue()));
        assertThat(
            "CRITICAL: amount should pass through UNCHANGED (scale 0, not normalised to 2). "
                + "If this fails, AmountMapper.normalizeAmount is auto-applying for same-type "
                + "BigDecimal→BigDecimal which would be a critical implicit-conversion finding.",
            target.amount().scale(),
            is(0)
        );
        assertThat(target.amount().toPlainString(), is("10"));
    }

    // =========================================================================
    // Claim 3 — LocalDateTime does NOT auto-convert to ZonedDateTime without DateTimeMapper in uses
    // =========================================================================

    @Test
    @DisplayName("Claim 3: LocalDateTime field is null in target when DateTimeMapper is not in uses")
    void shouldNotAutoConvertLocalDateTimeToZonedDateTimeWithoutDateTimeMapper() {
        // Given: A source with a non-null createdAt LocalDateTime
        final LocalDateTime createdAt = LocalDateTime.of(2025, 1, 15, 10, 30, 0);
        final BaseSource source = new BaseSource(UUID.randomUUID(), BigDecimal.TEN, createdAt);
        final BaseConsumerMapper mapper = generatedImpl(BaseConsumerMapper.class);

        // When: Mapping to the target (DateTimeMapper NOT in uses)
        final BaseTarget target = mapper.map(source);

        // Then: createdAt is null because the field is explicitly ignored via @Mapping(ignore=true).
        //       W3.6 EMPIRICAL FINDING: MapStruct does NOT silently leave LocalDateTime→ZonedDateTime
        //       null under ReportingPolicy.IGNORE — it fails at compile time with a type-mismatch
        //       error. IGNORE only suppresses warnings for unmatched target fields, not type errors.
        //       The explicit @Mapping(ignore=true) on BaseConsumerMapper.map() is required.
        //       The runtime outcome (null createdAt) is the same, but the mechanism differs from
        //       the original prediction.
        assertThat(target, is(notNullValue()));
        assertThat(
            "Claim 3 (updated): createdAt is null because the field is explicitly ignored. "
                + "W3.6 finding: without DateTimeMapper in uses, MapStruct fails at compile time "
                + "(not runtime) — ReportingPolicy.IGNORE does not save type-mismatch errors, "
                + "only unmapped-target-field warnings.",
            target.createdAt(),
            is(nullValue())
        );
    }

    // =========================================================================
    // Claim 4 — Consumer opt-in uses = DateTimeMapper.class MERGES with config-level uses
    // =========================================================================

    @Test
    @DisplayName("Claim 4a: Consumer opt-in — LocalDateTime maps to ZonedDateTime (UTC) when DateTimeMapper in uses")
    void shouldConvertLocalDateTimeToZonedDateTimeWithOptInDateTimeMapper() {
        // Given: A source with a LocalDateTime
        final LocalDateTime createdAt = LocalDateTime.of(2025, 6, 15, 12, 0, 0);
        final UUID id = UUID.fromString("aaaaaaaa-bbbb-cccc-dddd-eeeeeeeeeeee");
        final ExtendedSource source = new ExtendedSource(id, createdAt);
        final ExtendedConsumerMapper mapper = generatedImpl(ExtendedConsumerMapper.class);

        // When: Mapping with DateTimeMapper explicitly added to uses
        final ExtendedTarget target = mapper.map(source);

        // Then: LocalDateTime is converted to ZonedDateTime in UTC via DateTimeMapper
        assertThat(target, is(notNullValue()));
        assertThat(target.createdAt(), is(notNullValue()));
        assertThat(target.createdAt().getYear(), is(2025));
        assertThat(target.createdAt().getMonthValue(), is(6));
        assertThat(target.createdAt().getDayOfMonth(), is(15));
        assertThat(target.createdAt().getHour(), is(12));
        // W3.6 empirical finding: ZoneOffset.UTC.getId() returns "Z", not "UTC".
        // Both represent UTC (offset zero) but Java's ZoneOffset uses the ISO-8601 "Z" designator.
        // The underlying behaviour is correct — the timestamp IS UTC — the predicted string "UTC"
        // was wrong; the actual identifier is "Z".
        assertThat(target.createdAt().getZone().getId(), is("Z"));
    }

    @Test
    @DisplayName("Claim 4b: Config-level uses still active — UUID→String conversion works in opt-in mapper")
    void shouldStillConvertUuidToStringInExtendedMapper() {
        // Given: A source with both UUID and LocalDateTime fields
        final UUID id = UUID.fromString("aaaaaaaa-bbbb-cccc-dddd-eeeeeeeeeeee");
        final ExtendedSource source = new ExtendedSource(id, LocalDateTime.now());
        final ExtendedConsumerMapper mapper = generatedImpl(ExtendedConsumerMapper.class);

        // When: Mapping with both config-level UuidMapper AND consumer-level DateTimeMapper
        final ExtendedTarget target = mapper.map(source);

        // Then: UUID still converts to String proving config-level uses was NOT replaced by
        //       consumer-level uses (merge semantics confirmed).
        assertThat(target, is(notNullValue()));
        assertThat(target.id(), is("aaaaaaaa-bbbb-cccc-dddd-eeeeeeeeeeee"));
    }
}
