package io.github.jframe.http;

import io.github.support.UnitTest;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Tests for {@link HttpStatusCode}.
 *
 * <p>Verifies the HttpStatusCode enum functionality including:
 * <ul>
 * <li>Enum values exist with correct codes</li>
 * <li>getCode() returns the correct integer HTTP status code</li>
 * <li>getReason() returns a human-readable reason string</li>
 * <li>valueOf(int) static factory returns the correct enum constant</li>
 * <li>valueOf(int) throws IllegalArgumentException for unknown codes</li>
 * <li>Category methods: is2xxSuccessful(), is4xxClientError(), is5xxServerError()</li>
 * </ul>
 */
@DisplayName("HTTP - HttpStatusCode")
public class HttpStatusCodeTest extends UnitTest {

    // ---- Enum values exist ------------------------------------------------

    @Test
    @DisplayName("Should define OK with code 200")
    public void shouldDefineOkWithCode200() {
        // Given: The OK enum constant

        // When: Accessing OK
        final HttpStatusCode status = HttpStatusCode.OK;

        // Then: Code is 200
        assertThat(status, is(notNullValue()));
        assertThat(status.getCode(), is(equalTo(200)));
    }

    @Test
    @DisplayName("Should define BAD_REQUEST with code 400")
    public void shouldDefineBadRequestWithCode400() {
        // Given: The BAD_REQUEST enum constant

        // When: Accessing BAD_REQUEST
        final HttpStatusCode status = HttpStatusCode.BAD_REQUEST;

        // Then: Code is 400
        assertThat(status.getCode(), is(equalTo(400)));
    }

    @Test
    @DisplayName("Should define UNAUTHORIZED with code 401")
    public void shouldDefineUnauthorizedWithCode401() {
        // Given: The UNAUTHORIZED enum constant

        // When: Accessing UNAUTHORIZED
        final HttpStatusCode status = HttpStatusCode.UNAUTHORIZED;

        // Then: Code is 401
        assertThat(status.getCode(), is(equalTo(401)));
    }

    @Test
    @DisplayName("Should define NOT_FOUND with code 404")
    public void shouldDefineNotFoundWithCode404() {
        // Given: The NOT_FOUND enum constant

        // When: Accessing NOT_FOUND
        final HttpStatusCode status = HttpStatusCode.NOT_FOUND;

        // Then: Code is 404
        assertThat(status.getCode(), is(equalTo(404)));
    }

    @Test
    @DisplayName("Should define TOO_MANY_REQUESTS with code 429")
    public void shouldDefineTooManyRequestsWithCode429() {
        // Given: The TOO_MANY_REQUESTS enum constant

        // When: Accessing TOO_MANY_REQUESTS
        final HttpStatusCode status = HttpStatusCode.TOO_MANY_REQUESTS;

        // Then: Code is 429
        assertThat(status.getCode(), is(equalTo(429)));
    }

    @Test
    @DisplayName("Should define INTERNAL_SERVER_ERROR with code 500")
    public void shouldDefineInternalServerErrorWithCode500() {
        // Given: The INTERNAL_SERVER_ERROR enum constant

        // When: Accessing INTERNAL_SERVER_ERROR
        final HttpStatusCode status = HttpStatusCode.INTERNAL_SERVER_ERROR;

        // Then: Code is 500
        assertThat(status.getCode(), is(equalTo(500)));
    }

    // ---- getReason() ------------------------------------------------------

    @Test
    @DisplayName("Should return non-null reason for OK")
    public void shouldReturnReasonForOk() {
        // Given: The OK status

        // When: Calling getReason()
        final String reason = HttpStatusCode.OK.getReason();

        // Then: Reason is a non-null, non-blank human-readable string
        assertThat(reason, is(notNullValue()));
        assertThat(reason.isBlank(), is(false));
    }

    @Test
    @DisplayName("Should return non-null reason for BAD_REQUEST")
    public void shouldReturnReasonForBadRequest() {
        // Given: The BAD_REQUEST status

        // When: Calling getReason()
        final String reason = HttpStatusCode.BAD_REQUEST.getReason();

        // Then: Reason is a non-null, non-blank string
        assertThat(reason, is(notNullValue()));
        assertThat(reason.isBlank(), is(false));
    }

    @Test
    @DisplayName("Should return non-null reason for INTERNAL_SERVER_ERROR")
    public void shouldReturnReasonForInternalServerError() {
        // Given: The INTERNAL_SERVER_ERROR status

        // When: Calling getReason()
        final String reason = HttpStatusCode.INTERNAL_SERVER_ERROR.getReason();

        // Then: Reason is a non-null, non-blank string
        assertThat(reason, is(notNullValue()));
        assertThat(reason.isBlank(), is(false));
    }

    // ---- valueOf(int) static factory -------------------------------------

    @Test
    @DisplayName("Should return OK when valueOf called with 200")
    public void shouldReturnOkForCode200() {
        // Given: Status code 200

        // When: Calling valueOf(200)
        final HttpStatusCode status = HttpStatusCode.valueOf(200);

        // Then: Returns OK
        assertThat(status, is(equalTo(HttpStatusCode.OK)));
    }

    @Test
    @DisplayName("Should return BAD_REQUEST when valueOf called with 400")
    public void shouldReturnBadRequestForCode400() {
        // Given: Status code 400

        // When: Calling valueOf(400)
        final HttpStatusCode status = HttpStatusCode.valueOf(400);

        // Then: Returns BAD_REQUEST
        assertThat(status, is(equalTo(HttpStatusCode.BAD_REQUEST)));
    }

    @Test
    @DisplayName("Should return UNAUTHORIZED when valueOf called with 401")
    public void shouldReturnUnauthorizedForCode401() {
        // Given: Status code 401

        // When: Calling valueOf(401)
        final HttpStatusCode status = HttpStatusCode.valueOf(401);

        // Then: Returns UNAUTHORIZED
        assertThat(status, is(equalTo(HttpStatusCode.UNAUTHORIZED)));
    }

    @Test
    @DisplayName("Should return NOT_FOUND when valueOf called with 404")
    public void shouldReturnNotFoundForCode404() {
        // Given: Status code 404

        // When: Calling valueOf(404)
        final HttpStatusCode status = HttpStatusCode.valueOf(404);

        // Then: Returns NOT_FOUND
        assertThat(status, is(equalTo(HttpStatusCode.NOT_FOUND)));
    }

    @Test
    @DisplayName("Should return TOO_MANY_REQUESTS when valueOf called with 429")
    public void shouldReturnTooManyRequestsForCode429() {
        // Given: Status code 429

        // When: Calling valueOf(429)
        final HttpStatusCode status = HttpStatusCode.valueOf(429);

        // Then: Returns TOO_MANY_REQUESTS
        assertThat(status, is(equalTo(HttpStatusCode.TOO_MANY_REQUESTS)));
    }

    @Test
    @DisplayName("Should return INTERNAL_SERVER_ERROR when valueOf called with 500")
    public void shouldReturnInternalServerErrorForCode500() {
        // Given: Status code 500

        // When: Calling valueOf(500)
        final HttpStatusCode status = HttpStatusCode.valueOf(500);

        // Then: Returns INTERNAL_SERVER_ERROR
        assertThat(status, is(equalTo(HttpStatusCode.INTERNAL_SERVER_ERROR)));
    }

    @Test
    @DisplayName("Should throw IllegalArgumentException for unknown status code")
    public void shouldThrowForUnknownCode() {
        // Given: An unknown HTTP status code

        // When: Calling valueOf with an unknown code
        // Then: IllegalArgumentException is thrown
        assertThrows(IllegalArgumentException.class, () -> HttpStatusCode.valueOf(999));
    }

    @Test
    @DisplayName("Should throw IllegalArgumentException for zero code")
    public void shouldThrowForZeroCode() {
        // Given: Code zero

        // When / Then: Throws IllegalArgumentException
        assertThrows(IllegalArgumentException.class, () -> HttpStatusCode.valueOf(0));
    }

    @Test
    @DisplayName("Should throw IllegalArgumentException for negative code")
    public void shouldThrowForNegativeCode() {
        // Given: A negative code

        // When / Then: Throws IllegalArgumentException
        assertThrows(IllegalArgumentException.class, () -> HttpStatusCode.valueOf(-1));
    }

    // ---- Category methods ------------------------------------------------

    @Test
    @DisplayName("Should return true for is2xxSuccessful on OK")
    public void shouldBeSuccessfulForOk() {
        // Given: OK status

        // When: Checking category
        final boolean result = HttpStatusCode.OK.is2xxSuccessful();

        // Then: Is 2xx successful
        assertThat(result, is(true));
    }

    @Test
    @DisplayName("Should return false for is2xxSuccessful on BAD_REQUEST")
    public void shouldNotBeSuccessfulForBadRequest() {
        // Given: BAD_REQUEST status

        // When: Checking 2xx category
        final boolean result = HttpStatusCode.BAD_REQUEST.is2xxSuccessful();

        // Then: Is NOT 2xx
        assertThat(result, is(false));
    }

    @Test
    @DisplayName("Should return true for is4xxClientError on BAD_REQUEST")
    public void shouldBeClientErrorForBadRequest() {
        // Given: BAD_REQUEST status

        // When: Checking 4xx category
        final boolean result = HttpStatusCode.BAD_REQUEST.is4xxClientError();

        // Then: Is 4xx client error
        assertThat(result, is(true));
    }

    @Test
    @DisplayName("Should return true for is4xxClientError on UNAUTHORIZED")
    public void shouldBeClientErrorForUnauthorized() {
        // Given: UNAUTHORIZED status

        // When: Checking 4xx category
        final boolean result = HttpStatusCode.UNAUTHORIZED.is4xxClientError();

        // Then: Is 4xx client error
        assertThat(result, is(true));
    }

    @Test
    @DisplayName("Should return true for is4xxClientError on NOT_FOUND")
    public void shouldBeClientErrorForNotFound() {
        // Given: NOT_FOUND status

        // When: Checking 4xx category
        final boolean result = HttpStatusCode.NOT_FOUND.is4xxClientError();

        // Then: Is 4xx client error
        assertThat(result, is(true));
    }

    @Test
    @DisplayName("Should return true for is4xxClientError on TOO_MANY_REQUESTS")
    public void shouldBeClientErrorForTooManyRequests() {
        // Given: TOO_MANY_REQUESTS status

        // When: Checking 4xx category
        final boolean result = HttpStatusCode.TOO_MANY_REQUESTS.is4xxClientError();

        // Then: Is 4xx client error
        assertThat(result, is(true));
    }

    @Test
    @DisplayName("Should return false for is4xxClientError on OK")
    public void shouldNotBeClientErrorForOk() {
        // Given: OK status

        // When: Checking 4xx category
        final boolean result = HttpStatusCode.OK.is4xxClientError();

        // Then: Is NOT 4xx
        assertThat(result, is(false));
    }

    @Test
    @DisplayName("Should return true for is5xxServerError on INTERNAL_SERVER_ERROR")
    public void shouldBeServerErrorForInternalServerError() {
        // Given: INTERNAL_SERVER_ERROR status

        // When: Checking 5xx category
        final boolean result = HttpStatusCode.INTERNAL_SERVER_ERROR.is5xxServerError();

        // Then: Is 5xx server error
        assertThat(result, is(true));
    }

    @Test
    @DisplayName("Should return false for is5xxServerError on BAD_REQUEST")
    public void shouldNotBeServerErrorForBadRequest() {
        // Given: BAD_REQUEST status

        // When: Checking 5xx category
        final boolean result = HttpStatusCode.BAD_REQUEST.is5xxServerError();

        // Then: Is NOT 5xx
        assertThat(result, is(false));
    }

    @Test
    @DisplayName("Should return false for is5xxServerError on OK")
    public void shouldNotBeServerErrorForOk() {
        // Given: OK status

        // When: Checking 5xx category
        final boolean result = HttpStatusCode.OK.is5xxServerError();

        // Then: Is NOT 5xx
        assertThat(result, is(false));
    }

    @Test
    @DisplayName("Should have mutually exclusive category membership")
    public void shouldHaveMutuallyExclusiveCategories() {
        // Given: A known 2xx status

        // When: Checking all categories for OK
        final HttpStatusCode ok = HttpStatusCode.OK;

        // Then: Only 2xx returns true
        assertThat(ok.is2xxSuccessful(), is(true));
        assertThat(ok.is4xxClientError(), is(false));
        assertThat(ok.is5xxServerError(), is(false));
    }
}
