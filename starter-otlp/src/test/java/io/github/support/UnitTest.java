package io.github.support;

import io.github.jframe.logging.kibana.KibanaLogFields;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.net.URI;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.client.ClientHttpRequest;

import static io.github.jframe.logging.kibana.KibanaLogFieldNames.*;
import static org.mockito.Mockito.*;
import static org.mockito.Mockito.when;

/**
 * Base class for all unit tests in the JFrame OpenTelemetry starter module.
 *
 * <p>This class provides common setup and utility methods for unit tests.
 * All unit test classes should extend this class to ensure consistent test structure.
 *
 * <p>Test methods should follow the Given/When/Then pattern:
 * <ul>
 * <li><b>Given:</b> Set up test data and preconditions</li>
 * <li><b>When:</b> Execute the action being tested</li>
 * <li><b>Then:</b> Verify the expected outcome</li>
 * </ul>
 */
@ExtendWith(MockitoExtension.class)
public class UnitTest {

    protected static final int RESPONSE_LENGTH = 1000;
    protected static final String SERVICE_NAME_VALUE = "test-service";
    protected static final String TEST_TX_ID = "test-tx-id";
    protected static final String TEST_REQUEST_ID = "test-req-id";
    protected static final String TEST_TRACE_ID = "0123456789abcdef0123456789abcdef";
    protected static final String TEST_SPAN_ID = "0123456789abcdef";
    protected static final String REQUEST_BODY_STRING = "request-body";
    protected static final String L7_REQUEST_ID_VALUE = "l7-req-123";

    /**
     * Common setup executed before each test method.
     * Subclasses can override this method to add their own setup logic.
     */
    @BeforeEach
    public void setUp() {
        // Common setup for all tests
        // Subclasses can override and call super.setUp() if needed
    }

    @AfterEach
    public void tearDown() {
        KibanaLogFields.clear();
    }

    protected ClientHttpRequest aMockedClientHttpRequest() {
        final ClientHttpRequest request = mock(ClientHttpRequest.class);
        final HttpHeaders headers = spy(new HttpHeaders());

        when(request.getMethod()).thenReturn(HttpMethod.GET);
        when(request.getURI()).thenReturn(URI.create("http://example.com/api/test"));
        when(request.getHeaders()).thenReturn(headers);

        return request;
    }

    protected static byte[] aRequestBody() {
        return REQUEST_BODY_STRING.getBytes();
    }

    protected void setupKibanaFields() {
        KibanaLogFields.tag(TX_ID, TEST_TX_ID);
        KibanaLogFields.tag(REQUEST_ID, TEST_REQUEST_ID);
        KibanaLogFields.tag(TRACE_ID, TEST_TRACE_ID);
    }

    protected void setFieldValue(final Object target, final String fieldName, final Object value) {
        try {
            final Field field = target.getClass().getDeclaredField(fieldName);
            field.setAccessible(true);
            field.set(target, value);
        } catch (final Exception exception) {
            throw new RuntimeException("Failed to set field '" + fieldName + "' on " + target.getClass().getSimpleName(), exception);
        }
    }

    protected static InputStream anInputStreamWith(final String content) {
        return new ByteArrayInputStream(content.getBytes());
    }
}
