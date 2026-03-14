package io.github.jframe.http;

import io.github.support.UnitTest;

import jakarta.ws.rs.core.Response;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;

/**
 * Tests for {@link QuarkusHttpStatus}.
 *
 * <p>Verifies that each {@link HttpStatusCode} value is correctly mapped to its
 * corresponding JAX-RS {@link Response.Status}.
 */
@DisplayName("Quarkus - HTTP Status Converter")
public class QuarkusHttpStatusTest extends UnitTest {

    @Test
    @DisplayName("Should convert OK to 200 JAX-RS status")
    public void shouldConvertOkToJaxRsStatus() {
        // Given: The HttpStatusCode.OK enum value

        // When: Converting to JAX-RS Response.Status
        final Response.Status status = QuarkusHttpStatus.toJaxRsStatus(HttpStatusCode.OK);

        // Then: Status is 200 OK
        assertThat(status, is(notNullValue()));
        assertThat(status.getStatusCode(), is(equalTo(200)));
    }

    @Test
    @DisplayName("Should convert BAD_REQUEST to 400 JAX-RS status")
    public void shouldConvertBadRequestToJaxRsStatus() {
        // Given: The HttpStatusCode.BAD_REQUEST enum value

        // When: Converting to JAX-RS Response.Status
        final Response.Status status = QuarkusHttpStatus.toJaxRsStatus(HttpStatusCode.BAD_REQUEST);

        // Then: Status is 400 BAD_REQUEST
        assertThat(status, is(notNullValue()));
        assertThat(status.getStatusCode(), is(equalTo(400)));
    }

    @Test
    @DisplayName("Should convert UNAUTHORIZED to 401 JAX-RS status")
    public void shouldConvertUnauthorizedToJaxRsStatus() {
        // Given: The HttpStatusCode.UNAUTHORIZED enum value

        // When: Converting to JAX-RS Response.Status
        final Response.Status status = QuarkusHttpStatus.toJaxRsStatus(HttpStatusCode.UNAUTHORIZED);

        // Then: Status is 401 UNAUTHORIZED
        assertThat(status, is(notNullValue()));
        assertThat(status.getStatusCode(), is(equalTo(401)));
    }

    @Test
    @DisplayName("Should convert FORBIDDEN to 403 JAX-RS status")
    public void shouldConvertForbiddenToJaxRsStatus() {
        // Given: The HttpStatusCode.FORBIDDEN enum value

        // When: Converting to JAX-RS Response.Status
        final Response.Status status = QuarkusHttpStatus.toJaxRsStatus(HttpStatusCode.FORBIDDEN);

        // Then: Status is 403 FORBIDDEN
        assertThat(status, is(notNullValue()));
        assertThat(status.getStatusCode(), is(equalTo(403)));
    }

    @Test
    @DisplayName("Should convert NOT_FOUND to 404 JAX-RS status")
    public void shouldConvertNotFoundToJaxRsStatus() {
        // Given: The HttpStatusCode.NOT_FOUND enum value

        // When: Converting to JAX-RS Response.Status
        final Response.Status status = QuarkusHttpStatus.toJaxRsStatus(HttpStatusCode.NOT_FOUND);

        // Then: Status is 404 NOT_FOUND
        assertThat(status, is(notNullValue()));
        assertThat(status.getStatusCode(), is(equalTo(404)));
    }

    @Test
    @DisplayName("Should convert TOO_MANY_REQUESTS to 429 JAX-RS status code")
    public void shouldConvertTooManyRequestsToJaxRsStatus() {
        // Given: The HttpStatusCode.TOO_MANY_REQUESTS enum value

        // When: Converting to JAX-RS Response.Status
        final Response.Status status = QuarkusHttpStatus.toJaxRsStatus(HttpStatusCode.TOO_MANY_REQUESTS);

        // Then: Status is 429 TOO_MANY_REQUESTS
        assertThat(status, is(notNullValue()));
        assertThat(status.getStatusCode(), is(equalTo(429)));
    }

    @Test
    @DisplayName("Should convert INTERNAL_SERVER_ERROR to 500 JAX-RS status")
    public void shouldConvertInternalServerErrorToJaxRsStatus() {
        // Given: The HttpStatusCode.INTERNAL_SERVER_ERROR enum value

        // When: Converting to JAX-RS Response.Status
        final Response.Status status = QuarkusHttpStatus.toJaxRsStatus(HttpStatusCode.INTERNAL_SERVER_ERROR);

        // Then: Status is 500 INTERNAL_SERVER_ERROR
        assertThat(status, is(notNullValue()));
        assertThat(status.getStatusCode(), is(equalTo(500)));
    }

    @Test
    @DisplayName("Should return INTERNAL_SERVER_ERROR for unknown HttpStatusCode")
    public void shouldReturnInternalServerErrorForUnknownStatusCode() {
        // Given: A null HttpStatusCode (unknown / unmapped case)

        // When: Converting null to JAX-RS Response.Status
        final Response.Status status = QuarkusHttpStatus.toJaxRsStatus(null);

        // Then: Falls back to 500 INTERNAL_SERVER_ERROR
        assertThat(status, is(notNullValue()));
        assertThat(status.getStatusCode(), is(equalTo(500)));
    }
}
