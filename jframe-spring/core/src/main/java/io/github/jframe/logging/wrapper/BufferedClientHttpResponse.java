package io.github.jframe.logging.wrapper;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.CodingErrorAction;
import java.util.Arrays;

import org.jspecify.annotations.NonNull;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.util.StreamUtils;

import static java.nio.charset.StandardCharsets.UTF_8;

/**
 * Simple type of {@link ClientHttpResponse} that reads the response's body into memory, thus allowing for multiple invocations of
 * {@link #getBody()}.
 *
 * <p>When constructed via the cap-aware constructor, {@link #getBodyAsString()} returns only the first {@code byteCap}
 * bytes decoded as UTF-8. {@link #getBody()} always returns the full buffered body regardless of the cap so that
 * the response is delivered to the client unmodified.
 */
public final class BufferedClientHttpResponse implements ClientHttpResponse {

    /** Sentinel value indicating no cap. */
    private static final int UNLIMITED = -1;

    private final ClientHttpResponse delegate;

    /** Full buffered body — always complete. */
    private byte[] body;

    /**
     * Maximum bytes to expose via {@link #getBodyAsString()}.
     * {@code -1} means unlimited (returns the full string).
     */
    private final int byteCap;

    /**
     * Constructs a BufferedClientHttpResponse without a size cap.
     *
     * @param response the ClientHttpResponse to buffer
     */
    public BufferedClientHttpResponse(final ClientHttpResponse response) {
        this.delegate = response;
        this.byteCap = UNLIMITED;
    }

    /**
     * Cap-aware constructor. Buffers the full body but limits what {@link #getBodyAsString()} returns.
     *
     * @param response the ClientHttpResponse to buffer
     * @param byteCap  maximum bytes to include in the string returned by {@link #getBodyAsString()}.
     *                 Use {@code -1} for unlimited; {@code 0} returns an empty string from
     *                 {@link #getBodyAsString()} while the full body is still available via {@link #getBody()}.
     */
    public BufferedClientHttpResponse(final ClientHttpResponse response, final int byteCap) {
        this.delegate = response;
        this.byteCap = byteCap;
    }

    @NonNull
    @Override
    public HttpStatusCode getStatusCode() throws IOException {
        return this.delegate.getStatusCode();
    }

    @NonNull
    @Override
    public String getStatusText() throws IOException {
        return this.delegate.getStatusText();
    }

    @NonNull
    @Override
    public HttpHeaders getHeaders() {
        return this.delegate.getHeaders();
    }

    @NonNull
    @Override
    public InputStream getBody() throws IOException {
        if (this.body == null) {
            this.body = StreamUtils.copyToByteArray(this.delegate.getBody());
        }
        return new ByteArrayInputStream(this.body);
    }

    @Override
    public void close() {
        this.delegate.close();
    }

    /**
     * Returns the body of the response as a UTF-8 string.
     *
     * <p>When this instance was created via the cap-aware constructor, the returned string
     * contains at most {@code byteCap} decoded characters (the cap is applied at the byte
     * level before decoding, so multi-byte characters split at the boundary are silently
     * dropped rather than raising an exception).
     *
     * <p>The underlying buffered body is always complete; {@link #getBody()} is unaffected.
     *
     * @return the body as a (possibly capped) UTF-8 string
     * @throws IOException if an I/O error occurs while reading the body
     */
    public String getBodyAsString() throws IOException {
        final byte[] fullBytes = getBody().readAllBytes();
        return decodeWithCap(fullBytes, byteCap);
    }

    /**
     * Returns the capped body string (package-private — accessible to tests in the same package).
     *
     * @param maxBytes maximum bytes to include; {@code -1} for unlimited.
     * @return decoded (and possibly capped) string
     * @throws IOException if the body cannot be read
     */
    String getCappedBodyAsString(final int maxBytes) throws IOException {
        final byte[] fullBytes = getBody().readAllBytes();
        return decodeWithCap(fullBytes, maxBytes);
    }

    private static String decodeWithCap(final byte[] fullBytes, final int maxBytes) throws IOException {
        if (fullBytes == null || fullBytes.length == 0 || maxBytes == 0) {
            return "";
        }
        final byte[] slice = (maxBytes == UNLIMITED || maxBytes >= fullBytes.length)
            ? fullBytes
            : Arrays.copyOf(fullBytes, maxBytes);
        // Decode with REPLACE so a split multi-byte character does not throw
        return UTF_8.newDecoder()
            .onMalformedInput(CodingErrorAction.REPLACE)
            .onUnmappableCharacter(CodingErrorAction.REPLACE)
            .decode(java.nio.ByteBuffer.wrap(slice))
            .toString();
    }
}
