package io.github.jframe.http;

import io.github.support.UnitTest;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Tests for {@link HttpStatusCode}.
 *
 * <p>Verifies the HttpStatusCode enum functionality including:
 * <ul>
 * <li>valueOf(int) static factory returns the correct enum constant</li>
 * <li>valueOf(int) throws IllegalArgumentException for unknown codes</li>
 * <li>Category methods: is2xxSuccessful(), is4xxClientError(), is5xxServerError()</li>
 * </ul>
 */
@DisplayName("HTTP - HttpStatusCode")
public class HttpStatusCodeTest extends UnitTest {

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
