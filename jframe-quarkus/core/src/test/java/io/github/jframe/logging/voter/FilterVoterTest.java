package io.github.jframe.logging.voter;

import io.github.support.UnitTest;

import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.UriInfo;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link FilterVoter}.
 *
 * <p>Verifies FilterVoter decision logic including:
 * <ul>
 * <li>Combined evaluation of MediaTypeVoter and RequestVoter</li>
 * <li>Result caching in ContainerRequestContext properties</li>
 * <li>Null MediaType from context handled gracefully</li>
 * <li>Voters called only once per request context (cache on second call)</li>
 * </ul>
 */
@DisplayName("Quarkus Logging Voters - FilterVoter")
public class FilterVoterTest extends UnitTest {

    @Mock
    private MediaTypeVoter mediaTypeVoter;

    @Mock
    private RequestVoter requestVoter;

    @Mock
    private ContainerRequestContext requestContext;

    @Mock
    private UriInfo uriInfo;

    private FilterVoter filterVoter;

    @Override
    @BeforeEach
    public void setUp() {
        super.setUp();
        filterVoter = new FilterVoter(mediaTypeVoter, requestVoter);
    }

    // ---------------------------------------------------------------------------
    // Happy path — both voters agree
    // ---------------------------------------------------------------------------

    @Test
    @DisplayName("Should return true when both mediaTypeVoter and requestVoter return true")
    public void shouldReturnTrueWhenBothMediaTypeVoterAndRequestVoterReturnTrue() {
        // Given: No cached result; both voters return true
        when(requestContext.getProperty(anyString())).thenReturn(null);
        when(requestContext.getMediaType()).thenReturn(MediaType.APPLICATION_JSON_TYPE);
        when(requestContext.getUriInfo()).thenReturn(uriInfo);
        when(requestContext.getMethod()).thenReturn("GET");
        when(uriInfo.getPath()).thenReturn("/api/users");
        when(mediaTypeVoter.matches("application/json")).thenReturn(true);
        when(requestVoter.allowed("GET", "/api/users")).thenReturn(true);

        // When: Checking if filter is enabled
        final boolean result = filterVoter.enabled(requestContext);

        // Then: Filter is enabled
        assertThat(result, is(true));
        verify(requestContext).setProperty(anyString(), eq(true));
    }

    @Test
    @DisplayName("Should return false when mediaTypeVoter returns false")
    public void shouldReturnFalseWhenMediaTypeVoterReturnsFalse() {
        // Given: No cached result; media type voter rejects
        when(requestContext.getProperty(anyString())).thenReturn(null);
        when(requestContext.getMediaType()).thenReturn(MediaType.TEXT_HTML_TYPE);
        when(requestContext.getUriInfo()).thenReturn(uriInfo);
        when(requestContext.getMethod()).thenReturn("GET");
        when(uriInfo.getPath()).thenReturn("/api/users");
        when(mediaTypeVoter.matches("text/html")).thenReturn(false);
        when(requestVoter.allowed("GET", "/api/users")).thenReturn(true);

        // When: Checking if filter is enabled
        final boolean result = filterVoter.enabled(requestContext);

        // Then: Filter is disabled because media type is not allowed
        assertThat(result, is(false));
        verify(requestContext).setProperty(anyString(), eq(false));
    }

    @Test
    @DisplayName("Should return false when requestVoter returns false")
    public void shouldReturnFalseWhenRequestVoterReturnsFalse() {
        // Given: No cached result; request voter rejects (path excluded)
        when(requestContext.getProperty(anyString())).thenReturn(null);
        when(requestContext.getMediaType()).thenReturn(MediaType.APPLICATION_JSON_TYPE);
        when(requestContext.getUriInfo()).thenReturn(uriInfo);
        when(requestContext.getMethod()).thenReturn("GET");
        when(uriInfo.getPath()).thenReturn("/actuator/health");
        when(mediaTypeVoter.matches("application/json")).thenReturn(true);
        when(requestVoter.allowed("GET", "/actuator/health")).thenReturn(false);

        // When: Checking if filter is enabled
        final boolean result = filterVoter.enabled(requestContext);

        // Then: Filter is disabled because request path is excluded
        assertThat(result, is(false));
        verify(requestContext).setProperty(anyString(), eq(false));
    }

    @Test
    @DisplayName("Should return false when both mediaTypeVoter and requestVoter return false")
    public void shouldReturnFalseWhenBothMediaTypeVoterAndRequestVoterReturnFalse() {
        // Given: No cached result; both voters reject
        when(requestContext.getProperty(anyString())).thenReturn(null);
        when(requestContext.getMediaType()).thenReturn(MediaType.TEXT_HTML_TYPE);
        when(requestContext.getUriInfo()).thenReturn(uriInfo);
        when(requestContext.getMethod()).thenReturn("GET");
        when(uriInfo.getPath()).thenReturn("/actuator/health");
        when(mediaTypeVoter.matches("text/html")).thenReturn(false);
        when(requestVoter.allowed("GET", "/actuator/health")).thenReturn(false);

        // When: Checking if filter is enabled
        final boolean result = filterVoter.enabled(requestContext);

        // Then: Filter is disabled
        assertThat(result, is(false));
    }

    // ---------------------------------------------------------------------------
    // Caching behaviour
    // ---------------------------------------------------------------------------

    @Test
    @DisplayName("Should return cached true result without invoking voters again")
    public void shouldReturnCachedTrueResultWithoutInvokingVotersAgain() {
        // Given: A previously cached true result on the request context
        when(requestContext.getProperty(anyString())).thenReturn(Boolean.TRUE);

        // When: Checking if filter is enabled
        final boolean result = filterVoter.enabled(requestContext);

        // Then: Cached value is returned; voters are never invoked
        assertThat(result, is(true));
        verify(mediaTypeVoter, times(0)).matches(anyString());
        verify(requestVoter, times(0)).allowed(anyString(), anyString());
    }

    @Test
    @DisplayName("Should return cached false result without invoking voters again")
    public void shouldReturnCachedFalseResultWithoutInvokingVotersAgain() {
        // Given: A previously cached false result on the request context
        when(requestContext.getProperty(anyString())).thenReturn(Boolean.FALSE);

        // When: Checking if filter is enabled
        final boolean result = filterVoter.enabled(requestContext);

        // Then: Cached false value is returned; voters are never invoked
        assertThat(result, is(false));
        verify(mediaTypeVoter, times(0)).matches(anyString());
        verify(requestVoter, times(0)).allowed(anyString(), anyString());
    }

    @Test
    @DisplayName("Should cache result under package-based property key")
    public void shouldCacheResultUnderPackageBasedPropertyKey() {
        // Given: No cached result; both voters return true
        when(requestContext.getProperty(anyString())).thenReturn(null);
        when(requestContext.getMediaType()).thenReturn(MediaType.APPLICATION_JSON_TYPE);
        when(requestContext.getUriInfo()).thenReturn(uriInfo);
        when(requestContext.getMethod()).thenReturn("GET");
        when(uriInfo.getPath()).thenReturn("/api/users");
        when(mediaTypeVoter.matches("application/json")).thenReturn(true);
        when(requestVoter.allowed("GET", "/api/users")).thenReturn(true);

        // When: Checking if filter is enabled
        filterVoter.enabled(requestContext);

        // Then: Cache is written with the correct package-based key
        final String expectedKey = "io.github.jframe.logging.voter.FILTER_VOTER";
        verify(requestContext).getProperty(expectedKey);
        verify(requestContext).setProperty(expectedKey, true);
    }

    @Test
    @DisplayName("Should invoke voters exactly once when no cache is present")
    public void shouldInvokeVotersExactlyOnceWhenNoCacheIsPresent() {
        // Given: No cached result; both voters return true
        when(requestContext.getProperty(anyString())).thenReturn(null);
        when(requestContext.getMediaType()).thenReturn(MediaType.APPLICATION_JSON_TYPE);
        when(requestContext.getUriInfo()).thenReturn(uriInfo);
        when(requestContext.getMethod()).thenReturn("GET");
        when(uriInfo.getPath()).thenReturn("/api/users");
        when(mediaTypeVoter.matches("application/json")).thenReturn(true);
        when(requestVoter.allowed("GET", "/api/users")).thenReturn(true);

        // When: Checking if filter is enabled
        filterVoter.enabled(requestContext);

        // Then: Each voter was invoked exactly once
        verify(mediaTypeVoter, times(1)).matches("application/json");
        verify(requestVoter, times(1)).allowed("GET", "/api/users");
    }

    // ---------------------------------------------------------------------------
    // Null MediaType from context
    // ---------------------------------------------------------------------------

    @Test
    @DisplayName("Should handle null MediaType from ContainerRequestContext gracefully")
    public void shouldHandleNullMediaTypeFromContainerRequestContextGracefully() {
        // Given: No cached result; request context returns null media type
        when(requestContext.getProperty(anyString())).thenReturn(null);
        when(requestContext.getMediaType()).thenReturn(null);
        when(requestContext.getUriInfo()).thenReturn(uriInfo);
        when(requestContext.getMethod()).thenReturn("GET");
        when(uriInfo.getPath()).thenReturn("/api/users");
        when(mediaTypeVoter.matches(null)).thenReturn(true);
        when(requestVoter.allowed("GET", "/api/users")).thenReturn(true);

        // When: Checking if filter is enabled (should not throw)
        final boolean result = filterVoter.enabled(requestContext);

        // Then: Filter decision is based on voter responses with null content type
        assertThat(result, is(true));
        verify(mediaTypeVoter).matches(null);
    }

    @Test
    @DisplayName("Should pass null content type string to mediaTypeVoter when MediaType is null")
    public void shouldPassNullContentTypeStringToMediaTypeVoterWhenMediaTypeIsNull() {
        // Given: No cached result; null media type
        when(requestContext.getProperty(anyString())).thenReturn(null);
        when(requestContext.getMediaType()).thenReturn(null);
        when(requestContext.getUriInfo()).thenReturn(uriInfo);
        when(requestContext.getMethod()).thenReturn("POST");
        when(uriInfo.getPath()).thenReturn("/api/orders");
        when(mediaTypeVoter.matches((String) null)).thenReturn(false);
        when(requestVoter.allowed("POST", "/api/orders")).thenReturn(true);

        // When: Checking if filter is enabled
        final boolean result = filterVoter.enabled(requestContext);

        // Then: mediaTypeVoter receives null; combined result is false
        assertThat(result, is(false));
        verify(mediaTypeVoter).matches((String) null);
    }

    // ---------------------------------------------------------------------------
    // Both voters evaluated
    // ---------------------------------------------------------------------------

    @Test
    @DisplayName("Should evaluate both voters even when mediaTypeVoter returns true")
    public void shouldEvaluateBothVotersEvenWhenMediaTypeVoterReturnsTrue() {
        // Given: No cached result; media type voter allows but request voter rejects
        when(requestContext.getProperty(anyString())).thenReturn(null);
        when(requestContext.getMediaType()).thenReturn(MediaType.APPLICATION_JSON_TYPE);
        when(requestContext.getUriInfo()).thenReturn(uriInfo);
        when(requestContext.getMethod()).thenReturn("GET");
        when(uriInfo.getPath()).thenReturn("/actuator/info");
        when(mediaTypeVoter.matches("application/json")).thenReturn(true);
        when(requestVoter.allowed("GET", "/actuator/info")).thenReturn(false);

        // When: Checking if filter is enabled
        final boolean result = filterVoter.enabled(requestContext);

        // Then: Both voters were consulted; result is false
        assertThat(result, is(false));
        verify(mediaTypeVoter).matches("application/json");
        verify(requestVoter).allowed("GET", "/actuator/info");
    }
}
