package io.github.jframe.exception.page;

import io.github.jframe.exception.HttpException;
import io.github.jframe.exception.JFrameErrorCode;
import io.github.support.UnitTest;

import jakarta.ws.rs.core.Response;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.is;

@DisplayName("Unit Test - InvalidPageException")
class InvalidPageExceptionTest extends UnitTest {

    @Test
    @DisplayName("Should carry rejected parameter and value")
    void shouldCarryRejectedParameterAndValue() {
        // Given / When: An exception for a negative page number
        final InvalidPageException exception = new InvalidPageException("pageNumber", -3);

        // Then: All details are exposed
        assertThat(exception.getRejectedParameter(), is(equalTo("pageNumber")));
        assertThat(exception.getRejectedValue(), is(equalTo(-3)));
    }

    @Test
    @DisplayName("Should be a 400 HttpException with INVALID_PAGE error code")
    void shouldBeBadRequestHttpExceptionWithInvalidPageCode() {
        // Given / When: An exception for a negative page number
        final InvalidPageException exception = new InvalidPageException("pageNumber", -1);

        // Then: It maps to INVALID_PAGE / 400
        assertThat(exception, is(instanceOf(HttpException.class)));
        assertThat(exception.getErrorCode(), is(equalTo("INVALID_PAGE")));
        assertThat(exception.getHttpStatus(), is(equalTo(Response.Status.BAD_REQUEST)));
    }

    @Test
    @DisplayName("Should expose INVALID_PAGE error code constant")
    void shouldExposeInvalidPageConstant() {
        // Given / When / Then: The enum constant carries code, reason and status
        assertThat(JFrameErrorCode.INVALID_PAGE.getErrorCode(), is(equalTo("INVALID_PAGE")));
        assertThat(JFrameErrorCode.INVALID_PAGE.getReason(), is(equalTo("Invalid page number or size")));
        assertThat(JFrameErrorCode.INVALID_PAGE.getHttpStatus(), is(equalTo(Response.Status.BAD_REQUEST)));
    }
}
