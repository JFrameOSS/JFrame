package io.github.jframe.logging.wrapper;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Predicate;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.core.Cookie;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.MultivaluedMap;
import jakarta.ws.rs.core.Request;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.SecurityContext;
import jakarta.ws.rs.core.UriInfo;

/**
 * A {@link ContainerRequestContext} wrapper that eagerly buffers the entity stream into a byte
 * array on construction, enabling the body to be read multiple times.
 *
 * <p>All methods not related to body-buffering are delegated to the wrapped context.
 *
 * <p>The cap-aware constructor limits the logged copy to a configurable number of bytes while
 * always restoring the full body to the delegate's entity stream so the JAX-RS handler receives
 * the complete request.
 */
public final class CachingRequestContext implements ContainerRequestContext {

    private final ContainerRequestContext delegate;

    /** Full body — always the complete request bytes. */
    private final byte[] fullBody;

    /** Capped logging copy — may be shorter than {@link #fullBody}. */
    private final byte[] cachedBody;

    /**
     * Constructs a new {@code CachingRequestContext} wrapping the given delegate.
     *
     * <p>If the delegate has an entity, the entity stream is read in full and buffered. The
     * delegate's entity stream is then replaced with a fresh {@link ByteArrayInputStream} over the
     * buffered bytes so that downstream components can still read it.
     *
     * @param delegate the original {@link ContainerRequestContext} to wrap
     * @throws IOException if an I/O error occurs while reading the entity stream
     */
    public CachingRequestContext(final ContainerRequestContext delegate) throws IOException {
        this.delegate = delegate;
        if (delegate.hasEntity()) {
            this.fullBody = delegate.getEntityStream().readAllBytes();
            this.cachedBody = this.fullBody;
            delegate.setEntityStream(new ByteArrayInputStream(this.fullBody));
        } else {
            this.fullBody = new byte[0];
            this.cachedBody = new byte[0];
        }
    }

    /**
     * Cap-aware constructor. Reads the full body eagerly, stores a capped slice for logging, and
     * restores the full body to the delegate's entity stream so the JAX-RS handler reads the
     * complete request.
     *
     * @param delegate the original {@link ContainerRequestContext} to wrap
     * @param byteCap  maximum bytes to retain in the logging copy.
     *                 Use {@code -1} for unlimited; {@code 0} produces an empty logging copy.
     * @throws IOException if an I/O error occurs while reading the entity stream
     */
    public CachingRequestContext(final ContainerRequestContext delegate, final int byteCap) throws IOException {
        this.delegate = delegate;
        if (delegate.hasEntity()) {
            this.fullBody = delegate.getEntityStream().readAllBytes();
            this.cachedBody = applyByteCap(this.fullBody, byteCap);
            delegate.setEntityStream(new ByteArrayInputStream(this.fullBody));
        } else {
            this.fullBody = new byte[0];
            this.cachedBody = new byte[0];
        }
    }

    /**
     * Returns the buffered request body (capped logging copy) as a byte array.
     *
     * @return the cached body bytes; never {@code null}, empty array if no entity was present
     */
    public byte[] getCachedBody() {
        return Arrays.copyOf(cachedBody, cachedBody.length);
    }

    /**
     * Returns the buffered request body (capped logging copy) decoded as a UTF-8 string.
     *
     * @return the body as a UTF-8 string; empty string if no entity was present
     */
    public String getCachedBodyAsString() {
        return new String(cachedBody, StandardCharsets.UTF_8);
    }

    /**
     * Returns the buffered request body decoded using the specified charset.
     *
     * @param charset the charset name to use for decoding
     * @return the body as a string decoded with the given charset
     */
    public String getCachedBodyAsString(final String charset) {
        return new String(cachedBody, Charset.forName(charset));
    }

    private static byte[] applyByteCap(final byte[] data, final int byteCap) {
        if (data == null || data.length == 0 || byteCap == 0) {
            return new byte[0];
        }
        final int len = byteCap == -1 ? data.length : Math.min(data.length, byteCap);
        return Arrays.copyOf(data, len);
    }

    /**
     * Returns a fresh {@link InputStream} over the FULL body bytes.
     *
     * <p>Each call returns a new stream positioned at the beginning. The full (uncapped) body
     * is always returned so that downstream JAX-RS handlers receive the complete request.
     *
     * @return a new {@link ByteArrayInputStream} backed by the full body
     */
    @Override
    public InputStream getEntityStream() {
        return new ByteArrayInputStream(fullBody);
    }

    @Override
    public Object getProperty(final String name) {
        return delegate.getProperty(name);
    }

    @Override
    public Collection<String> getPropertyNames() {
        return delegate.getPropertyNames();
    }

    @Override
    public void setProperty(final String name, final Object object) {
        delegate.setProperty(name, object);
    }

    @Override
    public void removeProperty(final String name) {
        delegate.removeProperty(name);
    }

    @Override
    public UriInfo getUriInfo() {
        return delegate.getUriInfo();
    }

    @Override
    public void setRequestUri(final URI requestUri) {
        delegate.setRequestUri(requestUri);
    }

    @Override
    public void setRequestUri(final URI baseUri, final URI requestUri) {
        delegate.setRequestUri(baseUri, requestUri);
    }

    @Override
    public Request getRequest() {
        return delegate.getRequest();
    }

    @Override
    public String getMethod() {
        return delegate.getMethod();
    }

    @Override
    public void setMethod(final String method) {
        delegate.setMethod(method);
    }

    @Override
    public MultivaluedMap<String, String> getHeaders() {
        return delegate.getHeaders();
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
    public List<MediaType> getAcceptableMediaTypes() {
        return delegate.getAcceptableMediaTypes();
    }

    @Override
    public List<Locale> getAcceptableLanguages() {
        return delegate.getAcceptableLanguages();
    }

    @Override
    public Map<String, Cookie> getCookies() {
        return delegate.getCookies();
    }

    @Override
    public boolean hasEntity() {
        return delegate.hasEntity();
    }

    @Override
    public void setEntityStream(final InputStream input) {
        delegate.setEntityStream(input);
    }

    @Override
    public SecurityContext getSecurityContext() {
        return delegate.getSecurityContext();
    }

    @Override
    public void setSecurityContext(final SecurityContext context) {
        delegate.setSecurityContext(context);
    }

    @Override
    public void abortWith(final Response response) {
        delegate.abortWith(response);
    }
}
