package io.github.jframe.logging.voter;

import io.github.support.UnitTest;

import jakarta.servlet.http.HttpServletRequest;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.springframework.http.MediaType;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link FilterVoter}.
 *
 * <p>Verifies the FilterVoter functionality including:
 * <ul>
 * <li>Combining media type and request path voting</li>
 * <li>Caching decisions in request attributes</li>
 * <li>Handling both allowed and disallowed scenarios</li>
 * </ul>
 */
@DisplayName("Logging - FilterVoter")
class FilterVoterTest extends UnitTest {

    @Mock
    private MediaTypeVoter mediaTypeVoter;

    @Mock
    private RequestVoter requestVoter;

    @Mock
    private HttpServletRequest request;

    private FilterVoter filterVoter;

    @Override
    @BeforeEach
    public void setUp() {
        super.setUp();
        filterVoter = new FilterVoter(mediaTypeVoter, requestVoter);
    }

    @Test
    @DisplayName("Should return true when both media type and request are allowed")
    void enabled_withAllowedMediaTypeAndRequest_shouldReturnTrue() {
        // Given: Both media type and request are allowed
        when(request.getAttribute(anyString())).thenReturn(null);
        when(request.getContentType()).thenReturn(MediaType.APPLICATION_JSON_VALUE);
        when(mediaTypeVoter.mediaTypeMatches(MediaType.APPLICATION_JSON_VALUE)).thenReturn(true);
        when(requestVoter.allowed(request)).thenReturn(true);

        // When: Checking if filter is enabled
        final boolean enabled = filterVoter.enabled(request);

        // Then: Filter is enabled
        assertThat(enabled).isTrue();
        verify(request).setAttribute(anyString(), eq(true));
    }

    @Test
    @DisplayName("Should return false when media type is not allowed")
    void enabled_withDisallowedMediaType_shouldReturnFalse() {
        // Given: Media type is not allowed but request is allowed
        when(request.getAttribute(anyString())).thenReturn(null);
        when(request.getContentType()).thenReturn(MediaType.TEXT_HTML_VALUE);
        when(mediaTypeVoter.mediaTypeMatches(MediaType.TEXT_HTML_VALUE)).thenReturn(false);
        when(requestVoter.allowed(request)).thenReturn(true);

        // When: Checking if filter is enabled
        final boolean enabled = filterVoter.enabled(request);

        // Then: Filter is disabled due to media type
        assertThat(enabled).isFalse();
        verify(request).setAttribute(anyString(), eq(false));
    }

    @Test
    @DisplayName("Should return false when request is not allowed")
    void enabled_withDisallowedRequest_shouldReturnFalse() {
        // Given: Media type is allowed but request is not allowed
        when(request.getAttribute(anyString())).thenReturn(null);
        when(request.getContentType()).thenReturn(MediaType.APPLICATION_JSON_VALUE);
        when(mediaTypeVoter.mediaTypeMatches(MediaType.APPLICATION_JSON_VALUE)).thenReturn(true);
        when(requestVoter.allowed(request)).thenReturn(false);

        // When: Checking if filter is enabled
        final boolean enabled = filterVoter.enabled(request);

        // Then: Filter is disabled due to request path
        assertThat(enabled).isFalse();
        verify(request).setAttribute(anyString(), eq(false));
    }

    @Test
    @DisplayName("Should return false when both media type and request are not allowed")
    void enabled_withBothDisallowed_shouldReturnFalse() {
        // Given: Both media type and request are not allowed
        when(request.getAttribute(anyString())).thenReturn(null);
        when(request.getContentType()).thenReturn(MediaType.TEXT_HTML_VALUE);
        when(mediaTypeVoter.mediaTypeMatches(MediaType.TEXT_HTML_VALUE)).thenReturn(false);
        when(requestVoter.allowed(request)).thenReturn(false);

        // When: Checking if filter is enabled
        final boolean enabled = filterVoter.enabled(request);

        // Then: Filter is disabled
        assertThat(enabled).isFalse();
        verify(request).setAttribute(anyString(), eq(false));
    }

    @Test
    @DisplayName("Should return cached result from request attribute")
    void enabled_withCachedAttribute_shouldReturnCachedValue() {
        // Given: Filter decision is already cached in request attribute
        when(request.getAttribute(anyString())).thenReturn(true);

        // When: Checking if filter is enabled
        final boolean enabled = filterVoter.enabled(request);

        // Then: Cached value is returned without invoking voters
        assertThat(enabled).isTrue();
        verify(mediaTypeVoter, times(0)).mediaTypeMatches(anyString());
        verify(requestVoter, times(0)).allowed(any());
    }

    @Test
    @DisplayName("Should cache false result in request attribute")
    void enabled_withDisallowedResult_shouldCacheFalse() {
        // Given: Filter is disabled due to disallowed media type
        when(request.getAttribute(anyString())).thenReturn(null);
        when(request.getContentType()).thenReturn(MediaType.TEXT_HTML_VALUE);
        when(mediaTypeVoter.mediaTypeMatches(MediaType.TEXT_HTML_VALUE)).thenReturn(false);
        when(requestVoter.allowed(request)).thenReturn(true);

        // When: Checking if filter is enabled twice
        final boolean enabled1 = filterVoter.enabled(request);
        when(request.getAttribute(anyString())).thenReturn(false);
        final boolean enabled2 = filterVoter.enabled(request);

        // Then: Both calls return false and result is cached
        assertThat(enabled1).isFalse();
        assertThat(enabled2).isFalse();
        verify(mediaTypeVoter, times(1)).mediaTypeMatches(anyString());
        verify(requestVoter, times(1)).allowed(any());
    }

    @Test
    @DisplayName("Should use consistent attribute name for caching")
    void enabled_shouldUseSameAttributeNameForCaching() {
        // Given: Multiple calls to enabled
        when(request.getAttribute(anyString())).thenReturn(null);
        when(request.getContentType()).thenReturn(MediaType.APPLICATION_JSON_VALUE);
        when(mediaTypeVoter.mediaTypeMatches(MediaType.APPLICATION_JSON_VALUE)).thenReturn(true);
        when(requestVoter.allowed(request)).thenReturn(true);

        // When: Checking if filter is enabled
        filterVoter.enabled(request);

        // Then: Attribute is set with package-based name
        verify(request).getAttribute("io.github.jframe.logging.voter.FILTER_VOTER");
        verify(request).setAttribute("io.github.jframe.logging.voter.FILTER_VOTER", true);
    }

    @Test
    @DisplayName("Should handle null content type gracefully")
    void enabled_withNullContentType_shouldHandleGracefully() {
        // Given: Request has null content type
        when(request.getAttribute(anyString())).thenReturn(null);
        when(request.getContentType()).thenReturn(null);
        when(mediaTypeVoter.mediaTypeMatches((String) null)).thenReturn(true);
        when(requestVoter.allowed(request)).thenReturn(true);

        // When: Checking if filter is enabled
        final boolean enabled = filterVoter.enabled(request);

        // Then: Filter decision is based on voter responses
        assertThat(enabled).isTrue();
    }

    @Test
    @DisplayName("Should evaluate request voter even if media type is allowed")
    void enabled_shouldEvaluateBothVoters() {
        // Given: Media type is allowed and request voter needs to be checked
        when(request.getAttribute(anyString())).thenReturn(null);
        when(request.getContentType()).thenReturn(MediaType.APPLICATION_JSON_VALUE);
        when(mediaTypeVoter.mediaTypeMatches(MediaType.APPLICATION_JSON_VALUE)).thenReturn(true);
        when(requestVoter.allowed(request)).thenReturn(false);

        // When: Checking if filter is enabled
        final boolean enabled = filterVoter.enabled(request);

        // Then: Both voters are evaluated and result is false
        assertThat(enabled).isFalse();
        verify(mediaTypeVoter).mediaTypeMatches(MediaType.APPLICATION_JSON_VALUE);
        verify(requestVoter).allowed(request);
    }
}
