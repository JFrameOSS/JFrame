package io.github.jframe.tracing;

import io.github.support.UnitTest;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Tests for {@link OpenTelemetryConstants}.
 *
 * <p>Verifies that all constant values are correctly defined and the utility class
 * cannot be instantiated, including:
 * <ul>
 * <li>Logging inner class constants have expected values</li>
 * <li>Attributes inner class constants have expected values</li>
 * <li>Outer class has a private constructor (utility class pattern)</li>
 * </ul>
 */
@DisplayName("Unit Test - OpenTelemetry Constants")
public class OpenTelemetryConstantsTest extends UnitTest {

    // ─── Outer class instantiation guard ────────────────────────────────────

    @Test
    @DisplayName("Should throw exception when attempting to instantiate outer class via reflection")
    public void shouldThrowExceptionWhenAttemptingToInstantiateOuterClassViaReflection() throws Exception {
        // Given: The private constructor of OpenTelemetryConstants
        final Constructor<OpenTelemetryConstants> constructor =
            OpenTelemetryConstants.class.getDeclaredConstructor();
        constructor.setAccessible(true);

        // When: Attempting to invoke the private constructor
        // Then: An InvocationTargetException is thrown (wrapping UnsupportedOperationException or similar)
        assertThrows(InvocationTargetException.class, constructor::newInstance);
    }

    @Test
    @DisplayName("Should have a single private constructor in outer class")
    public void shouldHaveASinglePrivateConstructorInOuterClass() throws Exception {
        // Given: The constructors of OpenTelemetryConstants
        final Constructor<?>[] constructors = OpenTelemetryConstants.class.getDeclaredConstructors();

        // When: Inspecting the declared constructors
        // Then: There is exactly one constructor and it is private
        assertThat(constructors, is(notNullValue()));
        assertThat(constructors.length, is(equalTo(1)));
        assertThat(java.lang.reflect.Modifier.isPrivate(constructors[0].getModifiers()), is(true));
    }

    // ─── Logging constants ───────────────────────────────────────────────────

    @Test
    @DisplayName("Should have correct REQUEST_PREFIX value in Logging class")
    public void shouldHaveCorrectRequestPrefixValueInLoggingClass() {
        // Given: The Logging constants class

        // When: Reading the REQUEST_PREFIX constant
        final String requestPrefix = OpenTelemetryConstants.Logging.REQUEST_PREFIX;

        // Then: The value matches the expected logging prefix
        assertThat(requestPrefix, is(equalTo("[EXTERNAL] Outbound request is:")));
    }

    @Test
    @DisplayName("Should have correct RESPONSE_PREFIX value in Logging class")
    public void shouldHaveCorrectResponsePrefixValueInLoggingClass() {
        // Given: The Logging constants class

        // When: Reading the RESPONSE_PREFIX constant
        final String responsePrefix = OpenTelemetryConstants.Logging.RESPONSE_PREFIX;

        // Then: The value matches the expected logging prefix
        assertThat(responsePrefix, is(equalTo("[EXTERNAL] Incoming response is:")));
    }

    @Test
    @DisplayName("Should have correct LINE_BREAK value in Logging class")
    public void shouldHaveCorrectLineBreakValueInLoggingClass() {
        // Given: The Logging constants class

        // When: Reading the LINE_BREAK constant
        final String lineBreak = OpenTelemetryConstants.Logging.LINE_BREAK;

        // Then: The value is a newline character
        assertThat(lineBreak, is(equalTo("\n")));
    }

    @Test
    @DisplayName("Should have correct TAB value in Logging class")
    public void shouldHaveCorrectTabValueInLoggingClass() {
        // Given: The Logging constants class

        // When: Reading the TAB constant
        final String tab = OpenTelemetryConstants.Logging.TAB;

        // Then: The value is a tab character
        assertThat(tab, is(equalTo("\t")));
    }

    // ─── Attribute constants – application ──────────────────────────────────

    @Test
    @DisplayName("Should have correct SERVICE_NAME value in Attributes class")
    public void shouldHaveCorrectServiceNameValueInAttributesClass() {
        // Given: The Attributes constants class

        // When: Reading the SERVICE_NAME constant
        final String serviceName = OpenTelemetryConstants.Attributes.SERVICE_NAME;

        // Then: The value matches the expected OTEL attribute key
        assertThat(serviceName, is(equalTo("service.name")));
    }

    @Test
    @DisplayName("Should have correct EXCLUDE_TRACING value in Attributes class")
    public void shouldHaveCorrectExcludeTracingValueInAttributesClass() {
        // Given: The Attributes constants class

        // When: Reading the EXCLUDE_TRACING constant
        final String excludeTracing = OpenTelemetryConstants.Attributes.EXCLUDE_TRACING;

        // Then: The value matches the expected OTEL attribute key
        assertThat(excludeTracing, is(equalTo("otel.exclude")));
    }

    // ─── Attribute constants – error ─────────────────────────────────────────

    @Test
    @DisplayName("Should have correct ERROR value in Attributes class")
    public void shouldHaveCorrectErrorValueInAttributesClass() {
        // Given: The Attributes constants class

        // When: Reading the ERROR constant
        final String error = OpenTelemetryConstants.Attributes.ERROR;

        // Then: The value matches the expected OTEL attribute key
        assertThat(error, is(equalTo("error")));
    }

    @Test
    @DisplayName("Should have correct ERROR_TYPE value in Attributes class")
    public void shouldHaveCorrectErrorTypeValueInAttributesClass() {
        // Given: The Attributes constants class

        // When: Reading the ERROR_TYPE constant
        final String errorType = OpenTelemetryConstants.Attributes.ERROR_TYPE;

        // Then: The value matches the expected OTEL attribute key
        assertThat(errorType, is(equalTo("error.type")));
    }

    @Test
    @DisplayName("Should have correct ERROR_MESSAGE value in Attributes class")
    public void shouldHaveCorrectErrorMessageValueInAttributesClass() {
        // Given: The Attributes constants class

        // When: Reading the ERROR_MESSAGE constant
        final String errorMessage = OpenTelemetryConstants.Attributes.ERROR_MESSAGE;

        // Then: The value matches the expected OTEL attribute key
        assertThat(errorMessage, is(equalTo("error.message")));
    }

    // ─── Attribute constants – HTTP ──────────────────────────────────────────

    @Test
    @DisplayName("Should have correct HTTP_TRANSACTION_ID value in Attributes class")
    public void shouldHaveCorrectHttpTransactionIdValueInAttributesClass() {
        // Given: The Attributes constants class

        // When: Reading the HTTP_TRANSACTION_ID constant
        final String httpTransactionId = OpenTelemetryConstants.Attributes.HTTP_TRANSACTION_ID;

        // Then: The value matches the expected OTEL attribute key
        assertThat(httpTransactionId, is(equalTo("http.transaction_id")));
    }

    @Test
    @DisplayName("Should have correct HTTP_REQUEST_ID value in Attributes class")
    public void shouldHaveCorrectHttpRequestIdValueInAttributesClass() {
        // Given: The Attributes constants class

        // When: Reading the HTTP_REQUEST_ID constant
        final String httpRequestId = OpenTelemetryConstants.Attributes.HTTP_REQUEST_ID;

        // Then: The value matches the expected OTEL attribute key
        assertThat(httpRequestId, is(equalTo("http.request_id")));
    }

    @Test
    @DisplayName("Should have correct HTTP_URI value in Attributes class")
    public void shouldHaveCorrectHttpUriValueInAttributesClass() {
        // Given: The Attributes constants class

        // When: Reading the HTTP_URI constant
        final String httpUri = OpenTelemetryConstants.Attributes.HTTP_URI;

        // Then: The value matches the expected OTEL attribute key
        assertThat(httpUri, is(equalTo("http.uri")));
    }

    @Test
    @DisplayName("Should have correct HTTP_METHOD value in Attributes class")
    public void shouldHaveCorrectHttpMethodValueInAttributesClass() {
        // Given: The Attributes constants class

        // When: Reading the HTTP_METHOD constant
        final String httpMethod = OpenTelemetryConstants.Attributes.HTTP_METHOD;

        // Then: The value matches the expected OTEL attribute key
        assertThat(httpMethod, is(equalTo("http.method")));
    }

    @Test
    @DisplayName("Should have correct HTTP_STATUS_CODE value in Attributes class")
    public void shouldHaveCorrectHttpStatusCodeValueInAttributesClass() {
        // Given: The Attributes constants class

        // When: Reading the HTTP_STATUS_CODE constant
        final String httpStatusCode = OpenTelemetryConstants.Attributes.HTTP_STATUS_CODE;

        // Then: The value matches the expected OTEL attribute key
        assertThat(httpStatusCode, is(equalTo("http.status_code")));
    }

    @Test
    @DisplayName("Should have correct HTTP_CONTENT_TYPE value in Attributes class")
    public void shouldHaveCorrectHttpContentTypeValueInAttributesClass() {
        // Given: The Attributes constants class

        // When: Reading the HTTP_CONTENT_TYPE constant
        final String httpContentType = OpenTelemetryConstants.Attributes.HTTP_CONTENT_TYPE;

        // Then: The value matches the expected OTEL attribute key
        assertThat(httpContentType, is(equalTo("http.content_type")));
    }

    // ─── Attribute constants – external request/response ────────────────────

    @Test
    @DisplayName("Should have correct PEER_SERVICE value in Attributes class")
    public void shouldHaveCorrectPeerServiceValueInAttributesClass() {
        // Given: The Attributes constants class

        // When: Reading the PEER_SERVICE constant
        final String peerService = OpenTelemetryConstants.Attributes.PEER_SERVICE;

        // Then: The value matches the expected OTEL attribute key
        assertThat(peerService, is(equalTo("peer.service")));
    }

    @Test
    @DisplayName("Should have correct EXT_REQUEST_URI value in Attributes class")
    public void shouldHaveCorrectExtRequestUriValueInAttributesClass() {
        // Given: The Attributes constants class

        // When: Reading the EXT_REQUEST_URI constant
        final String extRequestUri = OpenTelemetryConstants.Attributes.EXT_REQUEST_URI;

        // Then: The value matches the expected OTEL attribute key
        assertThat(extRequestUri, is(equalTo("ext.request.uri")));
    }

    @Test
    @DisplayName("Should have correct EXT_RESPONSE_STATUS_CODE value in Attributes class")
    public void shouldHaveCorrectExtResponseStatusCodeValueInAttributesClass() {
        // Given: The Attributes constants class

        // When: Reading the EXT_RESPONSE_STATUS_CODE constant
        final String extResponseStatusCode = OpenTelemetryConstants.Attributes.EXT_RESPONSE_STATUS_CODE;

        // Then: The value matches the expected OTEL attribute key
        assertThat(extResponseStatusCode, is(equalTo("ext.response.status_code")));
    }
}
