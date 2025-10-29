package io.github.jframe.logging.kibana;

import io.github.support.UnitTest;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;

import static io.github.jframe.logging.kibana.KibanaLogFieldNames.*;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for {@link KibanaLogFields}.
 *
 * <p>Verifies the KibanaLogFields MDC operations including:
 * <ul>
 * <li>Setting log fields with different data types</li>
 * <li>Retrieving log field values</li>
 * <li>Clearing log fields</li>
 * <li>Auto-closeable log fields</li>
 * <li>Context management for thread-local logging</li>
 * <li>Generating log strings</li>
 * </ul>
 */
@DisplayName("Logging - KibanaLogFields")
class KibanaLogFieldsTest extends UnitTest {

    @AfterEach
    void tearDown() {
        // Clean up MDC after each test
        KibanaLogFields.clear();
    }

    @Test
    @DisplayName("Should set and retrieve string log field")
    void tag_withStringValue_shouldSetAndRetrieveValue() {
        // Given: A log field and string value
        final String value = "test-request-id";

        // When: Setting the field with string value
        KibanaLogFields.tag(REQUEST_ID, value);

        // Then: Value can be retrieved from MDC
        assertThat(KibanaLogFields.get(REQUEST_ID)).isEqualTo(value);
        assertThat(MDC.get(REQUEST_ID.getLogName())).isEqualTo(value);
    }

    @Test
    @DisplayName("Should set and retrieve integer log field")
    void tag_withIntValue_shouldSetAndRetrieveValue() {
        // Given: A log field and integer value
        final int value = 200;

        // When: Setting the field with integer value
        KibanaLogFields.tag(HTTP_STATUS, value);

        // Then: Value is stored as string and can be retrieved
        assertThat(KibanaLogFields.get(HTTP_STATUS)).isEqualTo("200");
    }

    @Test
    @DisplayName("Should set and retrieve enum log field")
    void tag_withEnumValue_shouldSetAndRetrieveValue() {
        // Given: A log field and enum value
        final TestEnum value = TestEnum.SUCCESS;

        // When: Setting the field with enum value
        KibanaLogFields.tag(TX_STATUS, value);

        // Then: Enum is converted to string and can be retrieved
        assertThat(KibanaLogFields.get(TX_STATUS)).isEqualTo("SUCCESS");
    }

    @Test
    @DisplayName("Should set and retrieve collection log field as formatted array")
    void tag_withCollection_shouldSetAsFormattedArray() {
        // Given: A log field and collection of values
        final List<String> values = Arrays.asList("header1", "header2", "header3");

        // When: Setting the field with collection
        KibanaLogFields.tag(TX_REQUEST_HEADERS, values);

        // Then: Collection is formatted as array string with quotes
        assertThat(KibanaLogFields.get(TX_REQUEST_HEADERS)).isEqualTo("['header1', 'header2', 'header3']");
    }

    @Test
    @DisplayName("Should clear field when setting blank string value")
    void tag_withBlankString_shouldClearField() {
        // Given: A field that has been set previously
        KibanaLogFields.tag(REQUEST_ID, "initial-value");

        // When: Setting the field with blank string
        KibanaLogFields.tag(REQUEST_ID, "   ");

        // Then: Field is cleared from MDC
        assertThat(KibanaLogFields.get(REQUEST_ID)).isNull();
    }

    @Test
    @DisplayName("Should clear field when setting null string value")
    void tag_withNullString_shouldClearField() {
        // Given: A field that has been set previously
        KibanaLogFields.tag(REQUEST_ID, "initial-value");

        // When: Setting the field with null string
        KibanaLogFields.tag(REQUEST_ID, (String) null);

        // Then: Field is cleared from MDC
        assertThat(KibanaLogFields.get(REQUEST_ID)).isNull();
    }

    @Test
    @DisplayName("Should clear field when setting null collection")
    void tag_withNullCollection_shouldClearField() {
        // Given: A field that has been set previously
        KibanaLogFields.tag(TX_REQUEST_HEADERS, Arrays.asList("header1", "header2"));

        // When: Setting the field with null collection
        KibanaLogFields.tag(TX_REQUEST_HEADERS, (List<String>) null);

        // Then: Field is cleared from MDC
        assertThat(KibanaLogFields.get(TX_REQUEST_HEADERS)).isNull();
    }

    @Test
    @DisplayName("Should clear field when setting empty collection")
    void tag_withEmptyCollection_shouldClearField() {
        // Given: A field that has been set previously
        KibanaLogFields.tag(TX_REQUEST_HEADERS, Arrays.asList("header1", "header2"));

        // When: Setting the field with empty collection
        KibanaLogFields.tag(TX_REQUEST_HEADERS, Collections.emptyList());

        // Then: Field is cleared from MDC
        assertThat(KibanaLogFields.get(TX_REQUEST_HEADERS)).isNull();
    }

    @Test
    @DisplayName("Should return null for field that is not set")
    void get_withUnsetField_shouldReturnNull() {
        // Given: A field that has not been set

        // When: Retrieving the unset field
        final String result = KibanaLogFields.get(REQUEST_ID);

        // Then: Null is returned
        assertThat(result).isNull();
    }

    @Test
    @DisplayName("Should return default value for field that is not set")
    void getOrDefault_withUnsetField_shouldReturnDefault() {
        // Given: A field that has not been set and a default value
        final String defaultValue = "default-id";

        // When: Retrieving field with default value
        final String result = KibanaLogFields.getOrDefault(REQUEST_ID, defaultValue);

        // Then: Default value is returned
        assertThat(result).isEqualTo(defaultValue);
    }

    @Test
    @DisplayName("Should return actual value instead of default when field is set")
    void getOrDefault_withSetField_shouldReturnActualValue() {
        // Given: A field that has been set and a default value
        final String actualValue = "actual-id";
        final String defaultValue = "default-id";
        KibanaLogFields.tag(REQUEST_ID, actualValue);

        // When: Retrieving field with default value
        final String result = KibanaLogFields.getOrDefault(REQUEST_ID, defaultValue);

        // Then: Actual value is returned, not default
        assertThat(result).isEqualTo(actualValue);
    }

    @Test
    @DisplayName("Should clear single log field")
    void clear_withSingleField_shouldRemoveFromMdc() {
        // Given: Multiple fields that have been set
        KibanaLogFields.tag(REQUEST_ID, "request-123");
        KibanaLogFields.tag(TX_ID, "transaction-456");

        // When: Clearing a single field
        KibanaLogFields.clear(REQUEST_ID);

        // Then: Specified field is cleared, others remain
        assertThat(KibanaLogFields.get(REQUEST_ID)).isNull();
        assertThat(KibanaLogFields.get(TX_ID)).isEqualTo("transaction-456");
    }

    @Test
    @DisplayName("Should clear multiple log fields with varargs")
    void clear_withMultipleFields_shouldRemoveAllFromMdc() {
        // Given: Multiple fields that have been set
        KibanaLogFields.tag(REQUEST_ID, "request-123");
        KibanaLogFields.tag(TX_ID, "transaction-456");
        KibanaLogFields.tag(SESSION_ID, "session-789");

        // When: Clearing multiple fields with varargs
        KibanaLogFields.clear(REQUEST_ID, TX_ID);

        // Then: Specified fields are cleared, others remain
        assertThat(KibanaLogFields.get(REQUEST_ID)).isNull();
        assertThat(KibanaLogFields.get(TX_ID)).isNull();
        assertThat(KibanaLogFields.get(SESSION_ID)).isEqualTo("session-789");
    }

    @Test
    @DisplayName("Should clear all log fields")
    void clear_withNoArgs_shouldRemoveAllFromMdc() {
        // Given: Multiple fields that have been set
        KibanaLogFields.tag(REQUEST_ID, "request-123");
        KibanaLogFields.tag(TX_ID, "transaction-456");
        KibanaLogFields.tag(SESSION_ID, "session-789");

        // When: Clearing all fields
        KibanaLogFields.clear();

        // Then: All fields are cleared from MDC
        assertThat(KibanaLogFields.get(REQUEST_ID)).isNull();
        assertThat(KibanaLogFields.get(TX_ID)).isNull();
        assertThat(KibanaLogFields.get(SESSION_ID)).isNull();
        assertThat(MDC.getCopyOfContextMap()).isNullOrEmpty();
    }

    @Test
    @DisplayName("Should create auto-closeable field with string value")
    @SuppressWarnings("try")
    void tagCloseable_withStringValue_shouldCreateAutoCloseable() {
        // Given: A log field and string value

        // When: Creating auto-closeable field and using it in try-with-resources
        try (AutoCloseableKibanaLogField field = KibanaLogFields.tagCloseable(REQUEST_ID, "request-123")) {
            // Then: Field is set within the try block
            assertThat(KibanaLogFields.get(REQUEST_ID)).isEqualTo("request-123");
        }

        // Then: Field is automatically cleared after try block
        assertThat(KibanaLogFields.get(REQUEST_ID)).isNull();
    }

    @Test
    @DisplayName("Should create auto-closeable field with integer value")
    @SuppressWarnings("try")
    void tagCloseable_withIntValue_shouldCreateAutoCloseable() {
        // Given: A log field and integer value

        // When: Creating auto-closeable field with integer
        try (AutoCloseableKibanaLogField field = KibanaLogFields.tagCloseable(HTTP_STATUS, 200)) {
            // Then: Field is set within the try block
            assertThat(KibanaLogFields.get(HTTP_STATUS)).isEqualTo("200");
        }

        // Then: Field is automatically cleared after try block
        assertThat(KibanaLogFields.get(HTTP_STATUS)).isNull();
    }

    @Test
    @DisplayName("Should create auto-closeable field with enum value")
    @SuppressWarnings("try")
    void tagCloseable_withEnumValue_shouldCreateAutoCloseable() {
        // Given: A log field and enum value

        // When: Creating auto-closeable field with enum
        try (AutoCloseableKibanaLogField field = KibanaLogFields.tagCloseable(TX_STATUS, TestEnum.SUCCESS)) {
            // Then: Field is set within the try block
            assertThat(KibanaLogFields.get(TX_STATUS)).isEqualTo("SUCCESS");
        }

        // Then: Field is automatically cleared after try block
        assertThat(KibanaLogFields.get(TX_STATUS)).isNull();
    }

    @Test
    @DisplayName("Should create auto-closeable field with collection value")
    @SuppressWarnings("try")
    void tagCloseable_withCollection_shouldCreateAutoCloseable() {
        // Given: A log field and collection value
        final List<String> headers = Arrays.asList("header1", "header2");

        // When: Creating auto-closeable field with collection
        try (AutoCloseableKibanaLogField field = KibanaLogFields.tagCloseable(TX_REQUEST_HEADERS, headers)) {
            // Then: Field is set with formatted array within the try block
            assertThat(KibanaLogFields.get(TX_REQUEST_HEADERS)).isEqualTo("['header1', 'header2']");
        }

        // Then: Field is automatically cleared after try block
        assertThat(KibanaLogFields.get(TX_REQUEST_HEADERS)).isNull();
    }

    @Test
    @DisplayName("Should generate log string from all MDC fields except LOG_TYPE")
    void getValuesAsLogString_withMultipleFields_shouldGenerateLogString() {
        // Given: Multiple fields set in MDC including LOG_TYPE
        KibanaLogFields.tag(REQUEST_ID, "req-123");
        KibanaLogFields.tag(TX_ID, "tx-456");
        KibanaLogFields.tag(LOG_TYPE, "HTTP");

        // When: Generating log string
        final String logString = KibanaLogFields.getValuesAsLogString();

        // Then: Log string contains all fields except LOG_TYPE
        assertThat(logString).contains("req_id=\"req-123\"");
        assertThat(logString).contains("tx_id=\"tx-456\"");
        assertThat(logString).doesNotContain("log_type");
    }


    @Test
    @DisplayName("Should create and retrieve context with current MDC state")
    void getContext_withFieldsSet_shouldCaptureCurrentState() {
        // Given: Fields set in current thread
        KibanaLogFields.tag(REQUEST_ID, "req-123");
        KibanaLogFields.tag(TX_ID, "tx-456");

        // When: Getting context
        final KibanaLogContext context = KibanaLogFields.getContext();

        // Then: Context captures current MDC state
        assertThat(context).isNotNull();
        assertThat(context.getContextMap()).containsEntry("req_id", "req-123");
        assertThat(context.getContextMap()).containsEntry("tx_id", "tx-456");
    }

    @Test
    @DisplayName("Should populate MDC from context")
    void populateFromContext_withValidContext_shouldRestoreMdcState() {
        // Given: A context with field values
        KibanaLogFields.tag(REQUEST_ID, "req-123");
        KibanaLogFields.tag(TX_ID, "tx-456");
        final KibanaLogContext context = KibanaLogFields.getContext();

        // Clear current MDC
        KibanaLogFields.clear();
        assertThat(KibanaLogFields.get(REQUEST_ID)).isNull();

        // When: Populating from saved context
        KibanaLogFields.populateFromContext(context);

        // Then: MDC is restored with context values
        assertThat(KibanaLogFields.get(REQUEST_ID)).isEqualTo("req-123");
        assertThat(KibanaLogFields.get(TX_ID)).isEqualTo("tx-456");
    }

    @Test
    @DisplayName("Should handle null context in populateFromContext")
    void populateFromContext_withNullContext_shouldNotThrowException() {
        // Given: Fields set in MDC
        KibanaLogFields.tag(REQUEST_ID, "req-123");

        // When: Populating from null context
        KibanaLogFields.populateFromContext(null);

        // Then: No exception is thrown and existing fields remain
        assertThat(KibanaLogFields.get(REQUEST_ID)).isEqualTo("req-123");
    }

    @Test
    @DisplayName("Should return field on tag operation for method chaining")
    void tag_withAnyValue_shouldReturnField() {
        // Given: A log field and value

        // When: Setting field with tag
        final KibanaLogField result = KibanaLogFields.tag(REQUEST_ID, "req-123");

        // Then: The field itself is returned for potential chaining
        assertThat(result).isEqualTo(REQUEST_ID);
    }

    // Test enum for testing enum values
    private enum TestEnum {
        SUCCESS,
        FAILURE
    }
}
