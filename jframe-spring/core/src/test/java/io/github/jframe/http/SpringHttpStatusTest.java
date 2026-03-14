package io.github.jframe.http;

import io.github.support.UnitTest;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Modifier;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Tests for {@link SpringHttpStatus}.
 *
 * <p>Verifies conversion between jframe-core's {@link HttpStatusCode} enum and
 * Spring's {@link HttpStatus}, including all 7 mapped values and error conditions.
 */
@DisplayName("Spring Core - SpringHttpStatus Converter")
public class SpringHttpStatusTest extends UnitTest {

    // -------------------------------------------------------------------------
    // Utility class structure
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("Should be a final class")
    public void shouldBeAFinalClass() {
        // Given: The SpringHttpStatus class

        // When: Checking the class modifiers
        final int modifiers = SpringHttpStatus.class.getModifiers();

        // Then: The class should be declared final
        assertThat(Modifier.isFinal(modifiers), is(true));
    }

    @Test
    @DisplayName("Should have private constructor preventing instantiation")
    public void shouldHavePrivateConstructorPreventingInstantiation() throws Exception {
        // Given: The single declared constructor of the utility class
        final Constructor<SpringHttpStatus> constructor =
            SpringHttpStatus.class.getDeclaredConstructor();

        // When: Checking constructor visibility
        final boolean isPrivate = Modifier.isPrivate(constructor.getModifiers());

        // Then: Constructor must be private
        assertThat(isPrivate, is(true));
    }

    @Test
    @DisplayName("Should throw exception when instantiated via reflection")
    public void shouldThrowExceptionWhenInstantiatedViaReflection() throws Exception {
        // Given: The private constructor made accessible via reflection
        final Constructor<SpringHttpStatus> constructor =
            SpringHttpStatus.class.getDeclaredConstructor();
        constructor.setAccessible(true);

        // When & Then: Attempting to instantiate should throw InvocationTargetException
        // (wrapping UnsupportedOperationException or similar)
        assertThrows(InvocationTargetException.class, constructor::newInstance);
    }

    // -------------------------------------------------------------------------
    // toSpringHttpStatus — forward mapping (all 7 enum values)
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("Should map OK to Spring HttpStatus.OK")
    public void shouldMapOkToSpringHttpStatusOk() {
        // Given: jframe HttpStatusCode.OK

        // When: Converting to Spring HttpStatus
        final HttpStatus result = SpringHttpStatus.toSpringHttpStatus(HttpStatusCode.OK);

        // Then: Spring HttpStatus.OK should be returned
        assertThat(result, is(equalTo(HttpStatus.OK)));
    }

    @Test
    @DisplayName("Should map BAD_REQUEST to Spring HttpStatus.BAD_REQUEST")
    public void shouldMapBadRequestToSpringHttpStatusBadRequest() {
        // Given: jframe HttpStatusCode.BAD_REQUEST

        // When: Converting to Spring HttpStatus
        final HttpStatus result = SpringHttpStatus.toSpringHttpStatus(HttpStatusCode.BAD_REQUEST);

        // Then: Spring HttpStatus.BAD_REQUEST should be returned
        assertThat(result, is(equalTo(HttpStatus.BAD_REQUEST)));
    }

    @Test
    @DisplayName("Should map UNAUTHORIZED to Spring HttpStatus.UNAUTHORIZED")
    public void shouldMapUnauthorizedToSpringHttpStatusUnauthorized() {
        // Given: jframe HttpStatusCode.UNAUTHORIZED

        // When: Converting to Spring HttpStatus
        final HttpStatus result = SpringHttpStatus.toSpringHttpStatus(HttpStatusCode.UNAUTHORIZED);

        // Then: Spring HttpStatus.UNAUTHORIZED should be returned
        assertThat(result, is(equalTo(HttpStatus.UNAUTHORIZED)));
    }

    @Test
    @DisplayName("Should map FORBIDDEN to Spring HttpStatus.FORBIDDEN")
    public void shouldMapForbiddenToSpringHttpStatusForbidden() {
        // Given: jframe HttpStatusCode.FORBIDDEN

        // When: Converting to Spring HttpStatus
        final HttpStatus result = SpringHttpStatus.toSpringHttpStatus(HttpStatusCode.FORBIDDEN);

        // Then: Spring HttpStatus.FORBIDDEN should be returned
        assertThat(result, is(equalTo(HttpStatus.FORBIDDEN)));
    }

    @Test
    @DisplayName("Should map NOT_FOUND to Spring HttpStatus.NOT_FOUND")
    public void shouldMapNotFoundToSpringHttpStatusNotFound() {
        // Given: jframe HttpStatusCode.NOT_FOUND

        // When: Converting to Spring HttpStatus
        final HttpStatus result = SpringHttpStatus.toSpringHttpStatus(HttpStatusCode.NOT_FOUND);

        // Then: Spring HttpStatus.NOT_FOUND should be returned
        assertThat(result, is(equalTo(HttpStatus.NOT_FOUND)));
    }

    @Test
    @DisplayName("Should map TOO_MANY_REQUESTS to Spring HttpStatus.TOO_MANY_REQUESTS")
    public void shouldMapTooManyRequestsToSpringHttpStatusTooManyRequests() {
        // Given: jframe HttpStatusCode.TOO_MANY_REQUESTS

        // When: Converting to Spring HttpStatus
        final HttpStatus result = SpringHttpStatus.toSpringHttpStatus(HttpStatusCode.TOO_MANY_REQUESTS);

        // Then: Spring HttpStatus.TOO_MANY_REQUESTS should be returned
        assertThat(result, is(equalTo(HttpStatus.TOO_MANY_REQUESTS)));
    }

    @Test
    @DisplayName("Should map INTERNAL_SERVER_ERROR to Spring HttpStatus.INTERNAL_SERVER_ERROR")
    public void shouldMapInternalServerErrorToSpringHttpStatusInternalServerError() {
        // Given: jframe HttpStatusCode.INTERNAL_SERVER_ERROR

        // When: Converting to Spring HttpStatus
        final HttpStatus result = SpringHttpStatus.toSpringHttpStatus(HttpStatusCode.INTERNAL_SERVER_ERROR);

        // Then: Spring HttpStatus.INTERNAL_SERVER_ERROR should be returned
        assertThat(result, is(equalTo(HttpStatus.INTERNAL_SERVER_ERROR)));
    }

    @Test
    @DisplayName("Should throw NullPointerException when toSpringHttpStatus receives null")
    public void shouldThrowNullPointerExceptionWhenToSpringHttpStatusReceivesNull() {
        // Given: A null jframe HttpStatusCode

        // When & Then: Should throw NullPointerException
        assertThrows(
            NullPointerException.class,
            () -> SpringHttpStatus.toSpringHttpStatus(null)
        );
    }

    // -------------------------------------------------------------------------
    // fromSpringHttpStatus — reverse mapping (all 7 enum values)
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("Should map Spring HttpStatus.OK to jframe HttpStatusCode.OK")
    public void shouldMapSpringHttpStatusOkToJframeOk() {
        // Given: Spring HttpStatus.OK

        // When: Converting to jframe HttpStatusCode
        final HttpStatusCode result = SpringHttpStatus.fromSpringHttpStatus(HttpStatus.OK);

        // Then: jframe HttpStatusCode.OK should be returned
        assertThat(result, is(equalTo(HttpStatusCode.OK)));
    }

    @Test
    @DisplayName("Should map Spring HttpStatus.BAD_REQUEST to jframe HttpStatusCode.BAD_REQUEST")
    public void shouldMapSpringHttpStatusBadRequestToJframeBadRequest() {
        // Given: Spring HttpStatus.BAD_REQUEST

        // When: Converting to jframe HttpStatusCode
        final HttpStatusCode result = SpringHttpStatus.fromSpringHttpStatus(HttpStatus.BAD_REQUEST);

        // Then: jframe HttpStatusCode.BAD_REQUEST should be returned
        assertThat(result, is(equalTo(HttpStatusCode.BAD_REQUEST)));
    }

    @Test
    @DisplayName("Should map Spring HttpStatus.UNAUTHORIZED to jframe HttpStatusCode.UNAUTHORIZED")
    public void shouldMapSpringHttpStatusUnauthorizedToJframeUnauthorized() {
        // Given: Spring HttpStatus.UNAUTHORIZED

        // When: Converting to jframe HttpStatusCode
        final HttpStatusCode result = SpringHttpStatus.fromSpringHttpStatus(HttpStatus.UNAUTHORIZED);

        // Then: jframe HttpStatusCode.UNAUTHORIZED should be returned
        assertThat(result, is(equalTo(HttpStatusCode.UNAUTHORIZED)));
    }

    @Test
    @DisplayName("Should map Spring HttpStatus.FORBIDDEN to jframe HttpStatusCode.FORBIDDEN")
    public void shouldMapSpringHttpStatusForbiddenToJframeForbidden() {
        // Given: Spring HttpStatus.FORBIDDEN

        // When: Converting to jframe HttpStatusCode
        final HttpStatusCode result = SpringHttpStatus.fromSpringHttpStatus(HttpStatus.FORBIDDEN);

        // Then: jframe HttpStatusCode.FORBIDDEN should be returned
        assertThat(result, is(equalTo(HttpStatusCode.FORBIDDEN)));
    }

    @Test
    @DisplayName("Should map Spring HttpStatus.NOT_FOUND to jframe HttpStatusCode.NOT_FOUND")
    public void shouldMapSpringHttpStatusNotFoundToJframeNotFound() {
        // Given: Spring HttpStatus.NOT_FOUND

        // When: Converting to jframe HttpStatusCode
        final HttpStatusCode result = SpringHttpStatus.fromSpringHttpStatus(HttpStatus.NOT_FOUND);

        // Then: jframe HttpStatusCode.NOT_FOUND should be returned
        assertThat(result, is(equalTo(HttpStatusCode.NOT_FOUND)));
    }

    @Test
    @DisplayName("Should map Spring HttpStatus.TOO_MANY_REQUESTS to jframe HttpStatusCode.TOO_MANY_REQUESTS")
    public void shouldMapSpringHttpStatusTooManyRequestsToJframeTooManyRequests() {
        // Given: Spring HttpStatus.TOO_MANY_REQUESTS

        // When: Converting to jframe HttpStatusCode
        final HttpStatusCode result = SpringHttpStatus.fromSpringHttpStatus(HttpStatus.TOO_MANY_REQUESTS);

        // Then: jframe HttpStatusCode.TOO_MANY_REQUESTS should be returned
        assertThat(result, is(equalTo(HttpStatusCode.TOO_MANY_REQUESTS)));
    }

    @Test
    @DisplayName("Should map Spring HttpStatus.INTERNAL_SERVER_ERROR to jframe HttpStatusCode.INTERNAL_SERVER_ERROR")
    public void shouldMapSpringHttpStatusInternalServerErrorToJframeInternalServerError() {
        // Given: Spring HttpStatus.INTERNAL_SERVER_ERROR

        // When: Converting to jframe HttpStatusCode
        final HttpStatusCode result = SpringHttpStatus.fromSpringHttpStatus(HttpStatus.INTERNAL_SERVER_ERROR);

        // Then: jframe HttpStatusCode.INTERNAL_SERVER_ERROR should be returned
        assertThat(result, is(equalTo(HttpStatusCode.INTERNAL_SERVER_ERROR)));
    }

    @Test
    @DisplayName("Should throw IllegalArgumentException when Spring status has no jframe mapping")
    public void shouldThrowIllegalArgumentExceptionWhenSpringStatusHasNoJframeMapping() {
        // Given: Spring HttpStatus.CONTINUE (100) — not present in jframe's HttpStatusCode

        // When & Then: Should throw IllegalArgumentException
        assertThrows(
            IllegalArgumentException.class,
            () -> SpringHttpStatus.fromSpringHttpStatus(HttpStatus.CONTINUE)
        );
    }

    @Test
    @DisplayName("Should throw NullPointerException when fromSpringHttpStatus receives null")
    public void shouldThrowNullPointerExceptionWhenFromSpringHttpStatusReceivesNull() {
        // Given: A null Spring HttpStatus

        // When & Then: Should throw NullPointerException
        assertThrows(
            NullPointerException.class,
            () -> SpringHttpStatus.fromSpringHttpStatus(null)
        );
    }

    // -------------------------------------------------------------------------
    // toSpringHttpStatusCode — Spring HttpStatusCode interface mapping
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("Should return Spring HttpStatusCode interface from toSpringHttpStatusCode")
    public void shouldReturnSpringHttpStatusCodeInterfaceFromToSpringHttpStatusCode() {
        // Given: jframe HttpStatusCode.OK

        // When: Converting to Spring's HttpStatusCode interface
        final org.springframework.http.HttpStatusCode result =
            SpringHttpStatus.toSpringHttpStatusCode(HttpStatusCode.OK);

        // Then: Result should not be null and should implement Spring's HttpStatusCode
        assertThat(result, is(notNullValue()));
        assertThat(result, is(instanceOf(org.springframework.http.HttpStatusCode.class)));
    }

    @Test
    @DisplayName("Should return correct status value from toSpringHttpStatusCode")
    public void shouldReturnCorrectStatusValueFromToSpringHttpStatusCode() {
        // Given: jframe HttpStatusCode.NOT_FOUND

        // When: Converting to Spring's HttpStatusCode interface
        final org.springframework.http.HttpStatusCode result =
            SpringHttpStatus.toSpringHttpStatusCode(HttpStatusCode.NOT_FOUND);

        // Then: The status code value should match 404
        assertThat(result.value(), is(equalTo(404)));
    }

    @Test
    @DisplayName("Should map all 7 jframe status codes via toSpringHttpStatusCode")
    public void shouldMapAll7JframeStatusCodesViaToSpringHttpStatusCode() {
        // Given: All 7 jframe HttpStatusCode enum values

        // When & Then: Each should be convertible without error, with the correct numeric code
        assertThat(SpringHttpStatus.toSpringHttpStatusCode(HttpStatusCode.OK).value(), is(equalTo(200)));
        assertThat(SpringHttpStatus.toSpringHttpStatusCode(HttpStatusCode.BAD_REQUEST).value(), is(equalTo(400)));
        assertThat(SpringHttpStatus.toSpringHttpStatusCode(HttpStatusCode.UNAUTHORIZED).value(), is(equalTo(401)));
        assertThat(SpringHttpStatus.toSpringHttpStatusCode(HttpStatusCode.FORBIDDEN).value(), is(equalTo(403)));
        assertThat(SpringHttpStatus.toSpringHttpStatusCode(HttpStatusCode.NOT_FOUND).value(), is(equalTo(404)));
        assertThat(SpringHttpStatus.toSpringHttpStatusCode(HttpStatusCode.TOO_MANY_REQUESTS).value(), is(equalTo(429)));
        assertThat(SpringHttpStatus.toSpringHttpStatusCode(HttpStatusCode.INTERNAL_SERVER_ERROR).value(), is(equalTo(500)));
    }

    @Test
    @DisplayName("Should throw NullPointerException when toSpringHttpStatusCode receives null")
    public void shouldThrowNullPointerExceptionWhenToSpringHttpStatusCodeReceivesNull() {
        // Given: A null jframe HttpStatusCode

        // When & Then: Should throw NullPointerException
        assertThrows(
            NullPointerException.class,
            () -> SpringHttpStatus.toSpringHttpStatusCode(null)
        );
    }
}
