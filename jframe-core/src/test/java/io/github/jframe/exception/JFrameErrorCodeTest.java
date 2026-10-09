package io.github.jframe.exception;

import io.github.support.UnitTest;

import jakarta.ws.rs.core.Response;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;

/**
 * Every errorCode jFrame itself can emit is a {@link JFrameErrorCode} constant.
 */
@DisplayName("Unit Test - JFrame error code completeness")
class JFrameErrorCodeTest extends UnitTest {

    @ParameterizedTest(name = "{0} -> {1}")
    @CsvSource(
        {
            "BAD_REQUEST, 400",
            "UNAUTHORIZED, 401",
            "FORBIDDEN, 403",
            "NOT_FOUND, 404",
            "METHOD_NOT_ALLOWED, 405",
            "NOT_ACCEPTABLE, 406",
            "UNSUPPORTED_MEDIA_TYPE, 415"
        }
    )
    @DisplayName("Should define status-derived codes with HTTP reason phrase as reason")
    void shouldDefineStatusDerivedCodeWithReasonPhrase(final String name, final int status) {
        // Given: The HTTP status jFrame maps framework/security errors to
        final Response.Status httpStatus = Response.Status.fromStatusCode(status);

        // When: Looking up the constant by name
        final JFrameErrorCode code = JFrameErrorCode.valueOf(name);

        // Then: Code equals name, status matches, reason is the reason phrase
        assertThat(code.getErrorCode(), is(equalTo(name)));
        assertThat(code.getHttpStatus(), is(httpStatus));
        assertThat(code.getReason(), is(equalTo(httpStatus.getReasonPhrase())));
    }

    @ParameterizedTest(name = "{0} -> {1}")
    @CsvSource(
        {
            "VALIDATION_ERROR, 400",
            "INVALID_SORT, 400",
            "INVALID_SEARCH, 400",
            "INVALID_PAGE, 400",
            "RATE_LIMIT_EXCEEDED, 429",
            "INTERNAL_SERVER_ERROR, 500"
        }
    )
    @DisplayName("Should define jFrame-specific codes with matching status")
    void shouldDefineJFrameSpecificCodeWithMatchingStatus(final String name, final int status) {
        // Given: A jFrame-specific code name
        // When: Looking up the constant
        final JFrameErrorCode code = JFrameErrorCode.valueOf(name);

        // Then: Code equals name and status matches
        assertThat(code.getErrorCode(), is(equalTo(name)));
        assertThat(code.getHttpStatus().getStatusCode(), is(status));
    }
}
