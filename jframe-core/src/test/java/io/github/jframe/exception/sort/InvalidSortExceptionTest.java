package io.github.jframe.exception.sort;

import io.github.jframe.exception.JFrameErrorCode;
import io.github.support.UnitTest;

import java.util.ArrayList;
import java.util.List;
import jakarta.ws.rs.core.Response;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;

@DisplayName("Unit Test - InvalidSortException")
class InvalidSortExceptionTest extends UnitTest {

    @Test
    @DisplayName("Should carry rejectedField and sortableFields")
    void shouldCarryRejectedFieldAndSortableFields() {
        // Given / When
        final var ex = new InvalidSortException("unknown", List.of("name", "createdAt"));

        // Then
        assertThat(ex.getRejectedField(), is(equalTo("unknown")));
        assertThat(ex.getSortableFields(), containsInAnyOrder("name", "createdAt"));
    }

    @Test
    @DisplayName("Should use INVALID_SORT error code")
    void shouldUseInvalidSortErrorCode() {
        // Given / When
        final var ex = new InvalidSortException("bad", List.of());

        // Then
        assertThat(ex.getErrorCode(), is(equalTo("INVALID_SORT")));
    }

    @Test
    @DisplayName("Should have 400 Bad Request HTTP status")
    void shouldHaveBadRequestStatus() {
        // Given / When
        final var ex = new InvalidSortException("bad", List.of("name"));

        // Then
        assertThat(ex.getHttpStatus(), is(equalTo(Response.Status.BAD_REQUEST)));
    }

    @Test
    @DisplayName("Should return immutable copy of sortableFields")
    void shouldReturnImmutableCopyOfSortableFields() {
        // Given
        final var mutable = new ArrayList<>(List.of("a", "b"));
        final var ex = new InvalidSortException("x", mutable);

        // When: mutate original list
        mutable.add("c");

        // Then: exception's list is unaffected
        assertThat(ex.getSortableFields().size(), is(2));
    }

    @Test
    @DisplayName("Should allow null rejectedField when sort is absent")
    void shouldAllowNullRejectedField() {
        // Given / When
        final var ex = new InvalidSortException(null, List.of());

        // Then: no NPE; rejectedField is null
        assertThat(ex.getRejectedField(), is(nullValue()));
    }

    @Test
    @DisplayName("Should expose JFrameErrorCode INVALID_SORT constant")
    void shouldExposeInvalidSortConstant() {
        // Given / When / Then
        assertThat(JFrameErrorCode.INVALID_SORT, is(notNullValue()));
        assertThat(JFrameErrorCode.INVALID_SORT.getHttpStatus(), is(Response.Status.BAD_REQUEST));
        assertThat(JFrameErrorCode.INVALID_SORT.getErrorCode(), is("INVALID_SORT"));
    }
}
