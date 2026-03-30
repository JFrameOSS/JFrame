package io.github.jframe.logging.ecs;

import io.github.support.UnitTest;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;

import static io.github.jframe.logging.ecs.EcsFieldNames.*;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

/**
 * Unit tests for {@link EcsFields}.
 *
 * <p>Verifies the EcsFields MDC operations including:
 * <ul>
 * <li>Setting log fields with different data types</li>
 * <li>Retrieving log field values</li>
 * <li>Clearing log fields</li>
 * <li>Auto-closeable log fields</li>
 * <li>Context management for thread-local logging</li>
 * <li>Generating log strings</li>
 * </ul>
 */
@DisplayName("Logging - EcsFields")
class EcsFieldsTest extends UnitTest {

    @AfterEach
    void tearDown() {
        // Clean up MDC after each test
        EcsFields.clear();
    }

    @Test
    @DisplayName("Should set and retrieve string log field")
    void tag_withStringValue_shouldSetAndRetrieveValue() {
        // Given: A log field and string value
        final String value = "test-request-id";

        // When: Setting the field with string value
        EcsFields.tag(REQUEST_ID, value);

        // Then: Value can be retrieved from MDC
        assertThat(EcsFields.get(REQUEST_ID), is(value));
        assertThat(MDC.get(REQUEST_ID.getKey()), is(value));
    }

    @Test
    @DisplayName("Should set and retrieve integer log field")
    void tag_withIntValue_shouldSetAndRetrieveValue() {
        // Given: A log field and integer value
        final int value = 200;

        // When: Setting the field with integer value
        EcsFields.tag(HTTP_STATUS, value);

        // Then: Value is stored as string and can be retrieved
        assertThat(EcsFields.get(HTTP_STATUS), is("200"));
    }

    @Test
    @DisplayName("Should set and retrieve enum log field")
    void tag_withEnumValue_shouldSetAndRetrieveValue() {
        // Given: A log field and enum value
        final TestEnum value = TestEnum.SUCCESS;

        // When: Setting the field with enum value
        EcsFields.tag(TX_STATUS, value);

        // Then: Enum is converted to string and can be retrieved
        assertThat(EcsFields.get(TX_STATUS), is("SUCCESS"));
    }

    @Test
    @DisplayName("Should set and retrieve collection log field as formatted array")
    void tag_withCollection_shouldSetAsFormattedArray() {
        // Given: A log field and collection of values
        final List<String> values = Arrays.asList("header1", "header2", "header3");

        // When: Setting the field with collection
        EcsFields.tag(TX_REQUEST_HEADERS, values);

        // Then: Collection is formatted as array string with quotes
        assertThat(EcsFields.get(TX_REQUEST_HEADERS), is("['header1', 'header2', 'header3']"));
    }

    @Test
    @DisplayName("Should clear field when setting blank string value")
    void tag_withBlankString_shouldClearField() {
        // Given: A field that has been set previously
        EcsFields.tag(REQUEST_ID, "initial-value");

        // When: Setting the field with blank string
        EcsFields.tag(REQUEST_ID, "   ");

        // Then: Field is cleared from MDC
        assertThat(EcsFields.get(REQUEST_ID), is(nullValue()));
    }

    @Test
    @DisplayName("Should clear field when setting null string value")
    void tag_withNullString_shouldClearField() {
        // Given: A field that has been set previously
        EcsFields.tag(REQUEST_ID, "initial-value");

        // When: Setting the field with null string
        EcsFields.tag(REQUEST_ID, (String) null);

        // Then: Field is cleared from MDC
        assertThat(EcsFields.get(REQUEST_ID), is(nullValue()));
    }

    @Test
    @DisplayName("Should clear field when setting null collection")
    void tag_withNullCollection_shouldClearField() {
        // Given: A field that has been set previously
        EcsFields.tag(TX_REQUEST_HEADERS, Arrays.asList("header1", "header2"));

        // When: Setting the field with null collection
        EcsFields.tag(TX_REQUEST_HEADERS, (List<String>) null);

        // Then: Field is cleared from MDC
        assertThat(EcsFields.get(TX_REQUEST_HEADERS), is(nullValue()));
    }

    @Test
    @DisplayName("Should clear field when setting empty collection")
    void tag_withEmptyCollection_shouldClearField() {
        // Given: A field that has been set previously
        EcsFields.tag(TX_REQUEST_HEADERS, Arrays.asList("header1", "header2"));

        // When: Setting the field with empty collection
        EcsFields.tag(TX_REQUEST_HEADERS, Collections.emptyList());

        // Then: Field is cleared from MDC
        assertThat(EcsFields.get(TX_REQUEST_HEADERS), is(nullValue()));
    }

    @Test
    @DisplayName("Should return null for field that is not set")
    void get_withUnsetField_shouldReturnNull() {
        // Given: A field that has not been set

        // When: Retrieving the unset field
        final String result = EcsFields.get(REQUEST_ID);

        // Then: Null is returned
        assertThat(result, is(nullValue()));
    }

    @Test
    @DisplayName("Should return default value for field that is not set")
    void getOrDefault_withUnsetField_shouldReturnDefault() {
        // Given: A field that has not been set and a default value
        final String defaultValue = "default-id";

        // When: Retrieving field with default value
        final String result = EcsFields.getOrDefault(REQUEST_ID, defaultValue);

        // Then: Default value is returned
        assertThat(result, is(defaultValue));
    }

    @Test
    @DisplayName("Should return actual value instead of default when field is set")
    void getOrDefault_withSetField_shouldReturnActualValue() {
        // Given: A field that has been set and a default value
        final String actualValue = "actual-id";
        final String defaultValue = "default-id";
        EcsFields.tag(REQUEST_ID, actualValue);

        // When: Retrieving field with default value
        final String result = EcsFields.getOrDefault(REQUEST_ID, defaultValue);

        // Then: Actual value is returned, not default
        assertThat(result, is(actualValue));
    }

    @Test
    @DisplayName("Should clear single log field")
    void clear_withSingleField_shouldRemoveFromMdc() {
        // Given: Multiple fields that have been set
        EcsFields.tag(REQUEST_ID, "request-123");
        EcsFields.tag(TX_ID, "transaction-456");

        // When: Clearing a single field
        EcsFields.clear(REQUEST_ID);

        // Then: Specified field is cleared, others remain
        assertThat(EcsFields.get(REQUEST_ID), is(nullValue()));
        assertThat(EcsFields.get(TX_ID), is("transaction-456"));
    }

    @Test
    @DisplayName("Should clear multiple log fields with varargs")
    void clear_withMultipleFields_shouldRemoveAllFromMdc() {
        // Given: Multiple fields that have been set
        EcsFields.tag(REQUEST_ID, "request-123");
        EcsFields.tag(TX_ID, "transaction-456");
        EcsFields.tag(SESSION_ID, "session-789");

        // When: Clearing multiple fields with varargs
        EcsFields.clear(REQUEST_ID, TX_ID);

        // Then: Specified fields are cleared, others remain
        assertThat(EcsFields.get(REQUEST_ID), is(nullValue()));
        assertThat(EcsFields.get(TX_ID), is(nullValue()));
        assertThat(EcsFields.get(SESSION_ID), is("session-789"));
    }

    @Test
    @DisplayName("Should clear all log fields")
    void clear_withNoArgs_shouldRemoveAllFromMdc() {
        // Given: Multiple fields that have been set
        EcsFields.tag(REQUEST_ID, "request-123");
        EcsFields.tag(TX_ID, "transaction-456");
        EcsFields.tag(SESSION_ID, "session-789");

        // When: Clearing all fields
        EcsFields.clear();

        // Then: All fields are cleared from MDC
        assertThat(EcsFields.get(REQUEST_ID), is(nullValue()));
        assertThat(EcsFields.get(TX_ID), is(nullValue()));
        assertThat(EcsFields.get(SESSION_ID), is(nullValue()));
        assertThat(MDC.getCopyOfContextMap(), anyOf(nullValue(), anEmptyMap()));
    }

    @Test
    @DisplayName("Should create auto-closeable field with string value")
    @SuppressWarnings("try")
    void tagCloseable_withStringValue_shouldCreateAutoCloseable() {
        // Given: A log field and string value

        // When: Creating auto-closeable field and using it in try-with-resources
        try (AutoCloseableEcsField field = EcsFields.tagCloseable(REQUEST_ID, "request-123")) {
            // Then: Field is set within the try block
            assertThat(EcsFields.get(REQUEST_ID), is("request-123"));
        }

        // Then: Field is automatically cleared after try block
        assertThat(EcsFields.get(REQUEST_ID), is(nullValue()));
    }

    @Test
    @DisplayName("Should create auto-closeable field with integer value")
    @SuppressWarnings("try")
    void tagCloseable_withIntValue_shouldCreateAutoCloseable() {
        // Given: A log field and integer value

        // When: Creating auto-closeable field with integer
        try (AutoCloseableEcsField field = EcsFields.tagCloseable(HTTP_STATUS, 200)) {
            // Then: Field is set within the try block
            assertThat(EcsFields.get(HTTP_STATUS), is("200"));
        }

        // Then: Field is automatically cleared after try block
        assertThat(EcsFields.get(HTTP_STATUS), is(nullValue()));
    }

    @Test
    @DisplayName("Should create auto-closeable field with enum value")
    @SuppressWarnings("try")
    void tagCloseable_withEnumValue_shouldCreateAutoCloseable() {
        // Given: A log field and enum value

        // When: Creating auto-closeable field with enum
        try (AutoCloseableEcsField field = EcsFields.tagCloseable(TX_STATUS, TestEnum.SUCCESS)) {
            // Then: Field is set within the try block
            assertThat(EcsFields.get(TX_STATUS), is("SUCCESS"));
        }

        // Then: Field is automatically cleared after try block
        assertThat(EcsFields.get(TX_STATUS), is(nullValue()));
    }

    @Test
    @DisplayName("Should create auto-closeable field with collection value")
    @SuppressWarnings("try")
    void tagCloseable_withCollection_shouldCreateAutoCloseable() {
        // Given: A log field and collection value
        final List<String> headers = Arrays.asList("header1", "header2");

        // When: Creating auto-closeable field with collection
        try (AutoCloseableEcsField field = EcsFields.tagCloseable(TX_REQUEST_HEADERS, headers)) {
            // Then: Field is set with formatted array within the try block
            assertThat(EcsFields.get(TX_REQUEST_HEADERS), is("['header1', 'header2']"));
        }

        // Then: Field is automatically cleared after try block
        assertThat(EcsFields.get(TX_REQUEST_HEADERS), is(nullValue()));
    }

    @Test
    @DisplayName("Should generate log string from all MDC fields except LOG_TYPE")
    void getValuesAsLogString_withMultipleFields_shouldGenerateLogString() {
        // Given: Multiple fields set in MDC including LOG_TYPE
        EcsFields.tag(REQUEST_ID, "req-123");
        EcsFields.tag(TX_ID, "tx-456");
        EcsFields.tag(LOG_TYPE, "HTTP");

        // When: Generating log string
        final String logString = EcsFields.getValuesAsLogString();

        // Then: Log string contains all fields except LOG_TYPE
        assertThat(logString, containsString(REQUEST_ID.getKey() + "=\"req-123\""));
        assertThat(logString, containsString(TX_ID.getKey() + "=\"tx-456\""));
        assertThat(logString, not(containsString(LOG_TYPE.getKey())));
    }


    @Test
    @DisplayName("Should create and retrieve context with current MDC state")
    void getMdcContext_withFieldsSet_shouldCaptureCurrentState() {
        // Given: Fields set in current thread
        EcsFields.tag(REQUEST_ID, "req-123");
        EcsFields.tag(TX_ID, "tx-456");

        // When: Getting context
        final MdcLogContext context = EcsFields.getMdcContext();

        // Then: Context captures current MDC state
        assertThat(context, is(notNullValue()));
        assertThat(context.getContextMap(), hasEntry(REQUEST_ID.getKey(), "req-123"));
        assertThat(context.getContextMap(), hasEntry(TX_ID.getKey(), "tx-456"));
    }

    @Test
    @DisplayName("Should populate MDC from context")
    void populateFromContext_withValidContext_shouldRestoreMdcState() {
        // Given: A context with field values
        EcsFields.tag(REQUEST_ID, "req-123");
        EcsFields.tag(TX_ID, "tx-456");
        final MdcLogContext context = EcsFields.getMdcContext();

        // Clear current MDC
        EcsFields.clear();
        assertThat(EcsFields.get(REQUEST_ID), is(nullValue()));

        // When: Populating from saved context
        EcsFields.populateFromContext(context);

        // Then: MDC is restored with context values
        assertThat(EcsFields.get(REQUEST_ID), is("req-123"));
        assertThat(EcsFields.get(TX_ID), is("tx-456"));
    }

    @Test
    @DisplayName("Should handle null context in populateFromContext")
    void populateFromContext_withNullContext_shouldNotThrowException() {
        // Given: Fields set in MDC
        EcsFields.tag(REQUEST_ID, "req-123");

        // When: Populating from null context
        EcsFields.populateFromContext(null);

        // Then: No exception is thrown and existing fields remain
        assertThat(EcsFields.get(REQUEST_ID), is("req-123"));
    }

    @Test
    @DisplayName("Should return field on tag operation for method chaining")
    void tag_withAnyValue_shouldReturnField() {
        // Given: A log field and value

        // When: Setting field with tag
        final EcsField result = EcsFields.tag(REQUEST_ID, "req-123");

        // Then: The field itself is returned for potential chaining
        assertThat(result, is(REQUEST_ID));
    }

    // Test enum for testing enum values
    private enum TestEnum {
        SUCCESS,
        FAILURE
    }
}
