package io.github.support;

import io.github.jframe.logging.kibana.KibanaLogFields;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import static io.github.jframe.logging.kibana.KibanaLogFieldNames.REQUEST_ID;
import static io.github.jframe.logging.kibana.KibanaLogFieldNames.TRACE_ID;
import static io.github.jframe.logging.kibana.KibanaLogFieldNames.TX_ID;

/**
 * Base class for all unit tests in the JFrame Quarkus OTLP module.
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

    protected static final String SERVICE_NAME_VALUE = "test-service";
    protected static final String TEST_TX_ID = "test-tx-id";
    protected static final String TEST_REQUEST_ID = "test-req-id";
    protected static final String TEST_TRACE_ID = "0123456789abcdef0123456789abcdef";
    protected static final String TEST_SPAN_ID = "0123456789abcdef";

    /**
     * Common setup executed before each test method.
     * Subclasses can override this method to add their own setup logic.
     */
    @BeforeEach
    public void setUp() {
        // Common setup for all tests
        // Subclasses can override and call super.setUp() if needed
    }

    /**
     * Cleans up KibanaLogFields after each test to prevent pollution.
     */
    @AfterEach
    public void tearDown() {
        KibanaLogFields.clear();
    }

    /**
     * Populates KibanaLogFields with standard test values for tracing.
     */
    protected void setupKibanaFields() {
        KibanaLogFields.tag(TX_ID, TEST_TX_ID);
        KibanaLogFields.tag(REQUEST_ID, TEST_REQUEST_ID);
        KibanaLogFields.tag(TRACE_ID, TEST_TRACE_ID);
    }
}
