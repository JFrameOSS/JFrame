package io.github.jframe.exception.search;

import io.github.jframe.exception.HttpException;
import io.github.jframe.exception.JFrameErrorCode;
import io.github.support.UnitTest;

import java.util.ArrayList;
import java.util.List;
import jakarta.ws.rs.core.Response;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;

@DisplayName("Unit Test - InvalidSearchException")
class InvalidSearchExceptionTest extends UnitTest {

    @Test
    @DisplayName("Should carry rejected field, value and searchable fields")
    void shouldCarryRejectedFieldValueAndSearchableFields() {
        // Given / When: An exception for an invalid value
        final InvalidSearchException exception = new InvalidSearchException("age", "abc", List.of("age", "name"));

        // Then: All details are exposed
        assertThat(exception.getRejectedField(), is(equalTo("age")));
        assertThat(exception.getRejectedValue(), is(equalTo("abc")));
        assertThat(exception.getSearchableFields(), contains("age", "name"));
    }

    @Test
    @DisplayName("Should be a 400 HttpException with INVALID_SEARCH error code")
    void shouldBeBadRequestHttpExceptionWithInvalidSearchCode() {
        // Given / When: An exception for an unknown field
        final InvalidSearchException exception = new InvalidSearchException("unknown", null, List.of("name"));

        // Then: It maps to INVALID_SEARCH / 400
        assertThat(exception, is(instanceOf(HttpException.class)));
        assertThat(exception.getErrorCode(), is(equalTo("INVALID_SEARCH")));
        assertThat(exception.getHttpStatus(), is(equalTo(Response.Status.BAD_REQUEST)));
        assertThat(exception.getRejectedValue(), is(nullValue()));
    }

    @Test
    @DisplayName("Should expose INVALID_SEARCH error code constant")
    void shouldExposeInvalidSearchConstant() {
        // Given / When / Then: The enum constant carries code, reason and status
        assertThat(JFrameErrorCode.INVALID_SEARCH.getErrorCode(), is(equalTo("INVALID_SEARCH")));
        assertThat(JFrameErrorCode.INVALID_SEARCH.getReason(), is(equalTo("Invalid search field or value")));
        assertThat(JFrameErrorCode.INVALID_SEARCH.getHttpStatus(), is(equalTo(Response.Status.BAD_REQUEST)));
    }

    @Test
    @DisplayName("Should defensively copy searchable fields")
    void shouldDefensivelyCopySearchableFields() {
        // Given: A mutable list
        final List<String> mutable = new ArrayList<>(List.of("a"));
        final InvalidSearchException exception = new InvalidSearchException("x", null, mutable);

        // When: Mutating the original list
        mutable.add("b");

        // Then: Exception is unaffected
        assertThat(exception.getSearchableFields(), contains("a"));
    }

    @Test
    @DisplayName("Should treat null searchable fields as empty list")
    void shouldTreatNullSearchableFieldsAsEmpty() {
        // Given / When: Null searchable fields
        final InvalidSearchException exception = new InvalidSearchException("x", null, null);

        // Then: Empty list, no NPE
        assertThat(exception.getSearchableFields(), is(empty()));
    }
}
