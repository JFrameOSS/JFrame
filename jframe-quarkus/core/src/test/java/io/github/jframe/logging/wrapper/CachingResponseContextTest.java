package io.github.jframe.logging.wrapper;

import io.github.support.UnitTest;

import java.nio.charset.StandardCharsets;
import jakarta.ws.rs.container.ContainerResponseContext;
import jakarta.ws.rs.core.MultivaluedMap;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link CachingResponseContext}.
 *
 * <p>Verifies body-capture behaviour including:
 * <ul>
 * <li>Returning cached body bytes via {@code getCachedBody()}</li>
 * <li>{@code getCachedBodyAsString()} conversion</li>
 * <li>{@code getContentLength()} returns cached-body length after capture</li>
 * <li>Graceful handling when body has not been captured yet</li>
 * <li>Setting cached body via {@code setCachedBody(byte[])}</li>
 * <li>Delegation of all other methods to the wrapped context</li>
 * </ul>
 */
@DisplayName("Unit Test - CachingResponseContext")
public class CachingResponseContextTest extends UnitTest {

    @Mock
    private ContainerResponseContext delegate;

    // ---------------------------------------------------------------------------
    // getCachedBody() — before and after capture
    // ---------------------------------------------------------------------------

    @Test
    @DisplayName("Should return null from getCachedBody before body is captured")
    public void shouldReturnNullFromGetCachedBodyBeforeBodyIsCaptured() {
        // Given: A wrapper around a response context with no body yet captured
        final CachingResponseContext context = new CachingResponseContext(delegate);

        // When: getCachedBody is called before setCachedBody
        final byte[] cachedBody = context.getCachedBody();

        // Then: Null is returned because the body has not been captured yet
        assertThat(cachedBody, is(nullValue()));
    }

    @Test
    @DisplayName("Should return stored bytes after setCachedBody is called")
    public void shouldReturnStoredBytesAfterSetCachedBodyIsCalled() {
        // Given: A wrapper and a byte array representing the serialized response body
        final byte[] responseBody = "{\"status\":\"ok\"}".getBytes(StandardCharsets.UTF_8);
        final CachingResponseContext context = new CachingResponseContext(delegate);

        // When: The captured body is set externally
        context.setCachedBody(responseBody);

        // Then: getCachedBody returns the exact bytes that were set
        assertThat(context.getCachedBody(), is(equalTo(responseBody)));
    }

    @Test
    @DisplayName("Should return same byte array reference on repeated calls to getCachedBody")
    public void shouldReturnSameByteArrayReferenceOnRepeatedCallsToGetCachedBody() {
        // Given: A wrapper with a cached body set
        final byte[] body = "consistent".getBytes(StandardCharsets.UTF_8);
        final CachingResponseContext context = new CachingResponseContext(delegate);
        context.setCachedBody(body);

        // When: getCachedBody is called multiple times
        final byte[] firstCall = context.getCachedBody();
        final byte[] secondCall = context.getCachedBody();

        // Then: Both calls return the same data
        assertThat(firstCall, is(equalTo(secondCall)));
    }

    @Test
    @DisplayName("Should accept null body bytes in setCachedBody without throwing")
    public void shouldAcceptNullBodyBytesInSetCachedBodyWithoutThrowing() {
        // Given: A wrapper with no prior cached body
        final CachingResponseContext context = new CachingResponseContext(delegate);

        // When: setCachedBody is called with null
        context.setCachedBody(null);

        // Then: getCachedBody returns null and no exception is thrown
        assertThat(context.getCachedBody(), is(nullValue()));
    }

    @Test
    @DisplayName("Should allow replacing cached body by calling setCachedBody again")
    public void shouldAllowReplacingCachedBodyByCallingSetCachedBodyAgain() {
        // Given: A wrapper with an initial cached body
        final byte[] firstBody = "first".getBytes(StandardCharsets.UTF_8);
        final byte[] secondBody = "second".getBytes(StandardCharsets.UTF_8);
        final CachingResponseContext context = new CachingResponseContext(delegate);
        context.setCachedBody(firstBody);

        // When: setCachedBody is called again with different bytes
        context.setCachedBody(secondBody);

        // Then: getCachedBody returns the latest set value
        assertThat(context.getCachedBody(), is(equalTo(secondBody)));
    }

    // ---------------------------------------------------------------------------
    // getCachedBodyAsString()
    // ---------------------------------------------------------------------------

    @Test
    @DisplayName("Should return UTF-8 string from getCachedBodyAsString after body is captured")
    public void shouldReturnUtf8StringFromGetCachedBodyAsStringAfterBodyIsCaptured() {
        // Given: A wrapper with a UTF-8 encoded cached body
        final String expectedBody = "{\"message\":\"created\"}";
        final CachingResponseContext context = new CachingResponseContext(delegate);
        context.setCachedBody(expectedBody.getBytes(StandardCharsets.UTF_8));

        // When: getCachedBodyAsString is called
        final String result = context.getCachedBodyAsString();

        // Then: The string matches the expected content
        assertThat(result, is(equalTo(expectedBody)));
    }

    @Test
    @DisplayName("Should return empty string from getCachedBodyAsString when cached body is empty array")
    public void shouldReturnEmptyStringFromGetCachedBodyAsStringWhenCachedBodyIsEmptyArray() {
        // Given: A wrapper with an empty byte array as cached body
        final CachingResponseContext context = new CachingResponseContext(delegate);
        context.setCachedBody(new byte[0]);

        // When: getCachedBodyAsString is called
        final String result = context.getCachedBodyAsString();

        // Then: An empty string is returned
        assertThat(result, is(equalTo("")));
    }

    @Test
    @DisplayName("Should return null from getCachedBodyAsString when no body has been captured")
    public void shouldReturnNullFromGetCachedBodyAsStringWhenNoBodyHasBeenCaptured() {
        // Given: A wrapper with no body set
        final CachingResponseContext context = new CachingResponseContext(delegate);

        // When: getCachedBodyAsString is called before any body capture
        final String result = context.getCachedBodyAsString();

        // Then: Null is returned because there is no body to convert
        assertThat(result, is(nullValue()));
    }

    // ---------------------------------------------------------------------------
    // getContentLength()
    // ---------------------------------------------------------------------------

    @Test
    @DisplayName("Should return length of cached body from getContentLength after body is captured")
    public void shouldReturnLengthOfCachedBodyFromGetContentLengthAfterBodyIsCaptured() {
        // Given: A wrapper with a known-size cached body
        final byte[] body = "hello world".getBytes(StandardCharsets.UTF_8);
        final CachingResponseContext context = new CachingResponseContext(delegate);
        context.setCachedBody(body);

        // When: getContentLength is called
        final int contentLength = context.getContentLength();

        // Then: The content length equals the cached body size
        assertThat(contentLength, is(equalTo(body.length)));
    }

    @Test
    @DisplayName("Should delegate getContentLength to delegate when no body has been cached")
    public void shouldDelegateGetContentLengthToDelegateWhenNoBodyHasBeenCached() {
        // Given: A wrapper with no cached body; delegate reports a content length
        when(delegate.getLength()).thenReturn(512);
        final CachingResponseContext context = new CachingResponseContext(delegate);

        // When: getContentLength is called
        final int contentLength = context.getContentLength();

        // Then: The delegate's content length is returned
        assertThat(contentLength, is(equalTo(512)));
    }

    @Test
    @DisplayName("Should return zero from getContentLength when cached body is empty array")
    public void shouldReturnZeroFromGetContentLengthWhenCachedBodyIsEmptyArray() {
        // Given: A wrapper with an empty cached body
        final CachingResponseContext context = new CachingResponseContext(delegate);
        context.setCachedBody(new byte[0]);

        // When: getContentLength is called
        final int contentLength = context.getContentLength();

        // Then: Zero is returned
        assertThat(contentLength, is(equalTo(0)));
    }

    // ---------------------------------------------------------------------------
    // Delegation
    // ---------------------------------------------------------------------------

    @Test
    @DisplayName("Should delegate getStatus to wrapped ContainerResponseContext")
    public void shouldDelegateGetStatusToWrappedContainerResponseContext() {
        // Given: A delegate configured to return HTTP 201
        when(delegate.getStatus()).thenReturn(201);
        final CachingResponseContext context = new CachingResponseContext(delegate);

        // When: getStatus is called on the wrapper
        final int status = context.getStatus();

        // Then: The delegate's status code is returned
        assertThat(status, is(equalTo(201)));
        verify(delegate).getStatus();
    }

    @Test
    @DisplayName("Should delegate getMediaType to wrapped ContainerResponseContext")
    public void shouldDelegateGetMediaTypeToWrappedContainerResponseContext() {
        // Given: A delegate with a known media type
        when(delegate.getMediaType()).thenReturn(jakarta.ws.rs.core.MediaType.APPLICATION_JSON_TYPE);
        final CachingResponseContext context = new CachingResponseContext(delegate);

        // When: getMediaType is called on the wrapper
        final jakarta.ws.rs.core.MediaType mediaType = context.getMediaType();

        // Then: The delegate's media type is returned
        assertThat(mediaType, is(notNullValue()));
        verify(delegate).getMediaType();
    }

    @Test
    @DisplayName("Should delegate getHeaders to wrapped ContainerResponseContext")
    public void shouldDelegateGetHeadersToWrappedContainerResponseContext() {
        // Given: A delegate with headers
        @SuppressWarnings("unchecked") final MultivaluedMap<String, Object> headers = mock(MultivaluedMap.class);
        when(delegate.getHeaders()).thenReturn(headers);
        final CachingResponseContext context = new CachingResponseContext(delegate);

        // When: getHeaders is called on the wrapper
        context.getHeaders();

        // Then: The call is forwarded to the delegate
        verify(delegate).getHeaders();
    }

    @Test
    @DisplayName("Should delegate getEntity to wrapped ContainerResponseContext")
    public void shouldDelegateGetEntityToWrappedContainerResponseContext() {
        // Given: A delegate with a response entity object
        final Object entity = new Object();
        when(delegate.getEntity()).thenReturn(entity);
        final CachingResponseContext context = new CachingResponseContext(delegate);

        // When: getEntity is called on the wrapper
        final Object result = context.getEntity();

        // Then: The delegate's entity is returned
        assertThat(result, is(equalTo(entity)));
        verify(delegate).getEntity();
    }

    @Test
    @DisplayName("Should delegate setStatus to wrapped ContainerResponseContext")
    public void shouldDelegateSetStatusToWrappedContainerResponseContext() {
        // Given: A caching wrapper
        final CachingResponseContext context = new CachingResponseContext(delegate);

        // When: setStatus is called on the wrapper
        context.setStatus(404);

        // Then: The call is forwarded to the delegate
        verify(delegate).setStatus(404);
    }

    // ---------------------------------------------------------------------------
    // Constructor
    // ---------------------------------------------------------------------------

    @Test
    @DisplayName("Should create CachingResponseContext successfully with valid delegate")
    public void shouldCreateCachingResponseContextSuccessfullyWithValidDelegate() {
        // Given: A valid ContainerResponseContext delegate

        // When: The wrapper is constructed
        final CachingResponseContext context = new CachingResponseContext(delegate);

        // Then: The wrapper instance is not null
        assertThat(context, is(notNullValue()));
    }
}
