package io.github.jframe.logging.wrapper;

import lombok.RequiredArgsConstructor;

import java.io.OutputStream;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Date;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;
import jakarta.ws.rs.container.ContainerResponseContext;
import jakarta.ws.rs.core.EntityTag;
import jakarta.ws.rs.core.Link;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.MultivaluedMap;
import jakarta.ws.rs.core.NewCookie;
import jakarta.ws.rs.core.Response;

/**
 * A {@link ContainerResponseContext} wrapper that supports externally capturing the serialized
 * response body as a byte array, enabling it to be read after the entity stream has been written.
 *
 * <p>The body is not captured automatically; callers must invoke {@link #setCachedBody(byte[])}
 * to store it. All other methods are delegated to the wrapped context.
 */
@RequiredArgsConstructor
public final class CachingResponseContext implements ContainerResponseContext {

    private final ContainerResponseContext delegate;
    private byte[] cachedBody;

    /**
     * Returns the cached response body bytes.
     *
     * @return the cached body, or {@code null} if {@link #setCachedBody(byte[])} has not been
     *         called yet
     */
    @SuppressWarnings("PMD.ReturnEmptyCollectionRatherThanNull")
    public byte[] getCachedBody() {
        if (cachedBody == null) {
            return null;
        }
        return Arrays.copyOf(cachedBody, cachedBody.length);
    }

    /**
     * Returns the cached response body decoded as a UTF-8 string.
     *
     * @return the body as a UTF-8 string, or {@code null} if no body has been captured yet
     */
    public String getCachedBodyAsString() {
        if (cachedBody == null) {
            return null;
        }
        return new String(cachedBody, StandardCharsets.UTF_8);
    }

    /**
     * Stores the serialized response body bytes.
     *
     * @param body the body bytes to cache; may be {@code null}
     */
    public void setCachedBody(final byte[] body) {
        this.cachedBody = body == null ? null : Arrays.copyOf(body, body.length);
    }

    /**
     * Returns the content length.
     *
     * <p>If a body has been cached, returns its byte-array length. Otherwise delegates to
     * {@link ContainerResponseContext#getLength()}.
     *
     * @return the content length in bytes
     */
    public int getContentLength() {
        if (cachedBody != null) {
            return cachedBody.length;
        }
        return delegate.getLength();
    }

    @Override
    public int getStatus() {
        return delegate.getStatus();
    }

    @Override
    public void setStatus(final int code) {
        delegate.setStatus(code);
    }

    @Override
    public Response.StatusType getStatusInfo() {
        return delegate.getStatusInfo();
    }

    @Override
    public void setStatusInfo(final Response.StatusType statusInfo) {
        delegate.setStatusInfo(statusInfo);
    }

    @Override
    public MultivaluedMap<String, Object> getHeaders() {
        return delegate.getHeaders();
    }

    @Override
    public MultivaluedMap<String, String> getStringHeaders() {
        return delegate.getStringHeaders();
    }

    @Override
    public String getHeaderString(final String name) {
        return delegate.getHeaderString(name);
    }

    @Override
    public boolean containsHeaderString(final String name, final String valueSeparatorRegex,
        final Predicate<String> valuePredicate) {
        return delegate.containsHeaderString(name, valueSeparatorRegex, valuePredicate);
    }

    @Override
    public Set<String> getAllowedMethods() {
        return delegate.getAllowedMethods();
    }

    @Override
    public Date getDate() {
        return delegate.getDate();
    }

    @Override
    public Locale getLanguage() {
        return delegate.getLanguage();
    }

    @Override
    public int getLength() {
        return delegate.getLength();
    }

    @Override
    public MediaType getMediaType() {
        return delegate.getMediaType();
    }

    @Override
    public Map<String, NewCookie> getCookies() {
        return delegate.getCookies();
    }

    @Override
    public EntityTag getEntityTag() {
        return delegate.getEntityTag();
    }

    @Override
    public Date getLastModified() {
        return delegate.getLastModified();
    }

    @Override
    public URI getLocation() {
        return delegate.getLocation();
    }

    @Override
    public Set<Link> getLinks() {
        return delegate.getLinks();
    }

    @Override
    public boolean hasLink(final String relation) {
        return delegate.hasLink(relation);
    }

    @Override
    public Link getLink(final String relation) {
        return delegate.getLink(relation);
    }

    @Override
    public Link.Builder getLinkBuilder(final String relation) {
        return delegate.getLinkBuilder(relation);
    }

    @Override
    public boolean hasEntity() {
        return delegate.hasEntity();
    }

    @Override
    public Object getEntity() {
        return delegate.getEntity();
    }

    @Override
    public Class<?> getEntityClass() {
        return delegate.getEntityClass();
    }

    @Override
    public Type getEntityType() {
        return delegate.getEntityType();
    }

    @Override
    public void setEntity(final Object entity) {
        delegate.setEntity(entity);
    }

    @Override
    public void setEntity(final Object entity, final Annotation[] annotations,
        final MediaType mediaType) {
        delegate.setEntity(entity, annotations, mediaType);
    }

    @Override
    public Annotation[] getEntityAnnotations() {
        return delegate.getEntityAnnotations();
    }

    @Override
    public OutputStream getEntityStream() {
        return delegate.getEntityStream();
    }

    @Override
    public void setEntityStream(final OutputStream outputStream) {
        delegate.setEntityStream(outputStream);
    }
}
