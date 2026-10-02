package io.github.jframe.exception.page;

import io.github.support.UnitTest;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;

@DisplayName("Unit Test - InvalidPageErrorResponseResource")
class InvalidPageErrorResponseResourceTest extends UnitTest {

    @Test
    @DisplayName("Should carry rejected parameter and value from exception")
    void shouldCarryDetailsFromException() {
        // Given: An invalid page exception
        final InvalidPageException exception = new InvalidPageException("pageNumber", -2);

        // When: Creating the response resource
        final InvalidPageErrorResponseResource resource = new InvalidPageErrorResponseResource(exception);

        // Then: Details are copied
        assertThat(resource.getRejectedParameter(), is(equalTo("pageNumber")));
        assertThat(resource.getRejectedValue(), is(equalTo(-2)));
    }
}
