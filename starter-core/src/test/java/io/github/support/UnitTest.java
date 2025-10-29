package io.github.support;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Base class for all unit tests in the JFrame framework.
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
     * Creates a test message with the given prefix and suffix.
     *
     * @param prefix the message prefix
     * @param suffix the message suffix
     * @return the constructed message
     */
    protected String createTestMessage(final String prefix, final String suffix) {
        return prefix + " " + suffix;
    }
}
