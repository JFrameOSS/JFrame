package io.github.jframe.logging.wrapper;

import io.github.support.UnitTest;

import java.nio.charset.StandardCharsets;
import java.util.Collections;
import jakarta.ws.rs.container.ContainerResponseContext;
import jakarta.ws.rs.core.EntityTag;
import jakarta.ws.rs.core.Link;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.MultivaluedMap;
import jakarta.ws.rs.core.NewCookie;
import jakarta.ws.rs.core.Response;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;

/**
 * EXISTING BEHAVIOUR tests for {@link CachingResponseContext}.
 *
 * <p>These compile and pass today. See {@link CachingResponseContextBodyCapTest}
 * for DRIVES FIX tests (compilation blockers).
 */
@DisplayName("Unit Test - CachingResponseContext existing correctness")
public class CachingResponseContextExistingBehaviourTest extends UnitTest {

    @Test
    @DisplayName("Should return full cached body with existing constructor (no cap)")
    public void shouldReturnFullCachedBodyWithExistingConstructor() {
        // Given: A response context with a 500-byte body
        final byte[] body = "A".repeat(500).getBytes(StandardCharsets.UTF_8);
        final CachingResponseContext context = new CachingResponseContext(new StubContainerResponseContext());

        // When: Storing and retrieving the body
        context.setCachedBody(body);

        // Then: Full 500 bytes returned
        assertThat(context.getCachedBody().length, is(equalTo(500)));
    }

    @Test
    @DisplayName("Should return null getCachedBody when nothing has been stored")
    public void shouldReturnNullWhenNothingHasBeenStored() {
        // Given: A fresh context with no body stored
        final CachingResponseContext context = new CachingResponseContext(new StubContainerResponseContext());

        // When: Retrieving before setCachedBody is called
        final byte[] retrieved = context.getCachedBody();

        // Then: null returned
        assertThat(retrieved, is(nullValue()));
    }

    @Test
    @DisplayName("Should return stable value across multiple getCachedBodyAsString calls")
    public void shouldReturnStableValueAcrossMultipleGetCachedBodyAsStringCalls() {
        // Given: A 50-byte body stored
        final String expected = "B".repeat(50);
        final CachingResponseContext context = new CachingResponseContext(new StubContainerResponseContext());
        context.setCachedBody(expected.getBytes(StandardCharsets.UTF_8));

        // When: getCachedBodyAsString called three times
        final String first = context.getCachedBodyAsString();
        final String second = context.getCachedBodyAsString();
        final String third = context.getCachedBodyAsString();

        // Then: All calls return identical correct value
        assertThat(first, is(equalTo(expected)));
        assertThat(second, is(equalTo(expected)));
        assertThat(third, is(equalTo(expected)));
    }

    /**
     * Regression guard: multiple getCachedBody() calls must return equal arrays.
     * Backend agent may optimise the repeated Arrays.copyOf away without changing semantics.
     */
    @Test
    @DisplayName("Should return equal arrays from multiple getCachedBody calls")
    public void shouldReturnEqualArraysFromMultipleGetCachedBodyCalls() {
        // Given: A 20-byte body stored
        final byte[] body = "C".repeat(20).getBytes(StandardCharsets.UTF_8);
        final CachingResponseContext context = new CachingResponseContext(new StubContainerResponseContext());
        context.setCachedBody(body);

        // When: getCachedBody called twice
        final byte[] first = context.getCachedBody();
        final byte[] second = context.getCachedBody();

        // Then: Both arrays contain the same data
        assertThat(first, is(equalTo(second)));
    }

    @Test
    @DisplayName("Should handle null body in setCachedBody without throwing")
    public void shouldHandleNullBodyInSetCachedBodyWithoutThrowing() {
        // Given: A context
        final CachingResponseContext context = new CachingResponseContext(new StubContainerResponseContext());

        // When: null body is stored
        context.setCachedBody(null);

        // Then: No exception; getCachedBody returns null
        assertThat(context.getCachedBody(), is(nullValue()));
    }

    // ---------------------------------------------------------------------------
    // Minimal stub for ContainerResponseContext
    // ---------------------------------------------------------------------------

    static final class StubContainerResponseContext implements ContainerResponseContext {

        @Override
        public int getStatus() { return 200; }

        @Override
        public void setStatus(final int code) {}

        @Override
        public Response.StatusType getStatusInfo() { return Response.Status.OK; }

        @Override
        public void setStatusInfo(final Response.StatusType info) {}

        @Override
        public MultivaluedMap<String, Object> getHeaders() { return null; }

        @Override
        public MultivaluedMap<String, String> getStringHeaders() { return null; }

        @Override
        public String getHeaderString(final String name) { return null; }

        @Override
        public boolean containsHeaderString(final String name, final String valueSeparatorRegex,
            final java.util.function.Predicate<String> valuePredicate) {
            return false;
        }

        @Override
        public java.util.Set<String> getAllowedMethods() { return Collections.emptySet(); }

        @Override
        public java.util.Date getDate() { return null; }

        @Override
        public java.util.Locale getLanguage() { return null; }

        @Override
        public int getLength() { return -1; }

        @Override
        public MediaType getMediaType() { return null; }

        @Override
        public java.util.Map<String, NewCookie> getCookies() { return Collections.emptyMap(); }

        @Override
        public EntityTag getEntityTag() { return null; }

        @Override
        public java.util.Date getLastModified() { return null; }

        @Override
        public java.net.URI getLocation() { return null; }

        @Override
        public java.util.Set<Link> getLinks() { return Collections.emptySet(); }

        @Override
        public boolean hasLink(final String relation) { return false; }

        @Override
        public Link getLink(final String relation) { return null; }

        @Override
        public Link.Builder getLinkBuilder(final String relation) { return null; }

        @Override
        public boolean hasEntity() { return false; }

        @Override
        public Object getEntity() { return null; }

        @Override
        public Class<?> getEntityClass() { return null; }

        @Override
        public java.lang.reflect.Type getEntityType() { return null; }

        @Override
        public java.lang.annotation.Annotation[] getEntityAnnotations() { return new java.lang.annotation.Annotation[0]; }

        @Override
        public java.io.OutputStream getEntityStream() { return null; }

        @Override
        public void setEntityStream(final java.io.OutputStream outputStream) {}

        @Override
        public void setEntity(final Object entity) {}

        @Override
        public void setEntity(final Object entity, final java.lang.annotation.Annotation[] annotations, final MediaType mediaType) {}
    }
}
