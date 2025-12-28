package io.github.jframe.logging.wrapper;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;

import org.jspecify.annotations.NonNull;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.util.StreamUtils;

import static java.nio.charset.StandardCharsets.UTF_8;

/**
 * Simple type of {@link ClientHttpResponse} that reads the response's body into memory, thus allowing for multiple invocations of
 * {@link #getBody()}.
 */
public final class BufferedClientHttpResponse implements ClientHttpResponse {

    private final ClientHttpResponse delegate;

    private byte[] body;

    /**
     * Constructs a BufferedClientHttpResponse by reading the body of the provided ClientHttpResponse.
     *
     * @param response the ClientHttpResponse to buffer
     */
    public BufferedClientHttpResponse(final ClientHttpResponse response) {
        this.delegate = response;
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
     * Returns the body of the response as a String, reading it from the buffered byte array. This method reads the body into memory, so it
     * can be called multiple times without re
     *
     * @return the body of the response as a String
     * @throws IOException if an I/O error occurs while reading the body
     */
    public String getBodyAsString() throws IOException {
        return new String(getBody().readAllBytes(), UTF_8);
    }
}
