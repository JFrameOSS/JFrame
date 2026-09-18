package io.github.jframe.datasource.search.model;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import io.github.jframe.datasource.search.SearchType;
import io.github.jframe.datasource.search.model.input.SortableColumn;
import io.github.support.UnitTest;

import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Sort;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;

@DisplayName("AbstractSortSearchMetaData Sorting Tests")
class AbstractSortSearchMetaDataSortingTest extends UnitTest {

    private ListAppender<ILoggingEvent> logAppender;

    @BeforeEach
    @Override
    public void setUp() {
        final Logger logger = (Logger) LoggerFactory.getLogger(AbstractSortSearchMetaData.class);
        logAppender = new ListAppender<>();
        logAppender.start();
        logger.addAppender(logAppender);
    }

    @AfterEach
    void tearDown() {
        final Logger logger = (Logger) LoggerFactory.getLogger(AbstractSortSearchMetaData.class);
        logger.detachAppender(logAppender);
    }

    // =========================================================================
    // Regression: valid-only paths must behave exactly as before
    // =========================================================================

    @Test
    @DisplayName("Should create Sort for single mapped field")
    void shouldReturnSortWhenSingleValidFieldRequested() {
        // Given: metadata with a sortable 'email' field mapped to 'user.email'
        final TestMetaData metaData = new TestMetaData();
        final List<SortableColumn> columns = List.of(new SortableColumn("email", "ASC"));

        // When: converting to Sort
        final Sort sort = metaData.toSort(columns);

        // Then: sort contains the DB column with the correct direction
        assertThat(sort, is(notNullValue()));
        final Sort.Order order = sort.getOrderFor("user.email");
        assertThat(order, is(notNullValue()));
        assertThat(order.getDirection(), is(equalTo(Sort.Direction.ASC)));
    }

    @Test
    @DisplayName("Should create Sort for multiple valid fields, preserving order and direction")
    void shouldReturnSortWhenMultipleValidFieldsRequested() {
        // Given: two sortable fields requested in a specific order
        final TestMetaData metaData = new TestMetaData();
        final List<SortableColumn> columns = List.of(
            new SortableColumn("email", "ASC"),
            new SortableColumn("role", "DESC")
        );

        // When: converting to Sort
        final Sort sort = metaData.toSort(columns);

        // Then: both columns are present with correct directions
        assertThat(sort, is(notNullValue()));
        assertThat(sort.stream().count(), is(2L));
        final Sort.Order emailOrder = sort.getOrderFor("user.email");
        assertThat(emailOrder, is(notNullValue()));
        assertThat(emailOrder.getDirection(), is(equalTo(Sort.Direction.ASC)));
        final Sort.Order roleOrder = sort.getOrderFor("role");
        assertThat(roleOrder, is(notNullValue()));
        assertThat(roleOrder.getDirection(), is(equalTo(Sort.Direction.DESC)));
    }

    @Test
    @DisplayName("Should return unsorted when sort list is empty")
    void shouldReturnUnsortedWhenEmptySortListProvided() {
        // Given: no sort columns requested
        final TestMetaData metaData = new TestMetaData();

        // When: converting an empty list
        final Sort sort = metaData.toSort(Collections.emptyList());

        // Then: result is unsorted, no warning logged
        assertThat(sort.isUnsorted(), is(true));
        assertThat(logAppender.list, hasSize(0));
    }

    // =========================================================================
    // New behaviour: invalid columns are stripped, not fatal
    // =========================================================================

    @Test
    @DisplayName("Should discard unregistered column and not throw")
    void shouldDiscardUnregisteredColumnAndNotThrowWhenAllColumnsUnknown() {
        // Given: 'unknown' is not registered in metadata at all
        final TestMetaData metaData = new TestMetaData();
        final List<SortableColumn> columns = List.of(new SortableColumn("unknown", "ASC"));

        // When: converting to Sort — must not throw
        final Sort sort = metaData.toSort(columns);

        // Then: result is unsorted (all columns discarded)
        assertThat(sort.isUnsorted(), is(true));
    }

    @Test
    @DisplayName("Should log a warning naming the unregistered column when it is discarded")
    void shouldLogWarningNamingDiscardedUnregisteredColumn() {
        // Given: 'unknown' is not registered in metadata at all
        final TestMetaData metaData = new TestMetaData();
        final List<SortableColumn> columns = List.of(new SortableColumn("unknown", "ASC"));

        // When: converting to Sort
        metaData.toSort(columns);

        // Then: a WARN-level message is emitted and it names the offending column
        final List<ILoggingEvent> warnings = logAppender.list.stream()
            .filter(e -> e.getLevel() == Level.WARN)
            .toList();
        assertThat("expected at least one WARN log", warnings, hasSize(1));
        assertThat(warnings.getFirst().getFormattedMessage(), containsString("unknown"));
    }

    @Test
    @DisplayName("Should discard non-sortable registered column and not throw")
    void shouldDiscardNonSortableColumnAndNotThrowWhenAllColumnsNonSortable() {
        // Given: 'nonSortableField' is registered but not marked sortable
        final TestMetaData metaData = new TestMetaData();
        final List<SortableColumn> columns = List.of(new SortableColumn("nonSortableField", "ASC"));

        // When: converting to Sort — must not throw
        final Sort sort = metaData.toSort(columns);

        // Then: result is unsorted (non-sortable column discarded)
        assertThat(sort.isUnsorted(), is(true));
    }

    @Test
    @DisplayName("Should log a warning naming the non-sortable column when it is discarded")
    void shouldLogWarningNamingDiscardedNonSortableColumn() {
        // Given: 'nonSortableField' is registered but not marked sortable
        final TestMetaData metaData = new TestMetaData();
        final List<SortableColumn> columns = List.of(new SortableColumn("nonSortableField", "ASC"));

        // When: converting to Sort
        metaData.toSort(columns);

        // Then: a WARN-level message is emitted and it names the offending column
        final List<ILoggingEvent> warnings = logAppender.list.stream()
            .filter(e -> e.getLevel() == Level.WARN)
            .toList();
        assertThat("expected at least one WARN log", warnings, hasSize(1));
        assertThat(warnings.getFirst().getFormattedMessage(), containsString("nonSortableField"));
    }

    @Test
    @DisplayName("Should return only valid column when mix of valid and invalid columns is requested")
    void shouldReturnOnlyValidColumnsWhenMixOfValidAndInvalidColumnsRequested() {
        // Given: 'email' is sortable, 'unknown' is not registered
        final TestMetaData metaData = new TestMetaData();
        final List<SortableColumn> columns = List.of(
            new SortableColumn("email", "ASC"),
            new SortableColumn("unknown", "DESC")
        );

        // When: converting to Sort — must not throw
        final Sort sort = metaData.toSort(columns);

        // Then: only 'email' survives, direction is preserved, unknown is absent
        assertThat(sort.isUnsorted(), is(false));
        assertThat(sort.stream().count(), is(1L));
        final Sort.Order emailOrder = sort.getOrderFor("user.email");
        assertThat(emailOrder, is(notNullValue()));
        assertThat(emailOrder.getDirection(), is(equalTo(Sort.Direction.ASC)));
    }

    @Test
    @DisplayName("Should log a warning naming the invalid column in a mixed request")
    void shouldLogWarningNamingDiscardedColumnInMixedRequest() {
        // Given: 'email' is sortable, 'unknown' is not registered
        final TestMetaData metaData = new TestMetaData();
        final List<SortableColumn> columns = List.of(
            new SortableColumn("email", "ASC"),
            new SortableColumn("unknown", "DESC")
        );

        // When: converting to Sort
        metaData.toSort(columns);

        // Then: warning is logged naming the bad column, not the good one
        final List<ILoggingEvent> warnings = logAppender.list.stream()
            .filter(e -> e.getLevel() == Level.WARN)
            .toList();
        assertThat("expected at least one WARN log", warnings, hasSize(1));
        assertThat(warnings.getFirst().getFormattedMessage(), containsString("unknown"));
    }

    @Test
    @DisplayName("Should preserve direction of surviving valid column in a mixed request")
    void shouldPreserveDirectionOfSurvivingValidColumnInMixedRequest() {
        // Given: 'role' is sortable with DESC direction; 'nonSortableField' is registered but non-sortable
        final TestMetaData metaData = new TestMetaData();
        final List<SortableColumn> columns = List.of(
            new SortableColumn("role", "DESC"),
            new SortableColumn("nonSortableField", "ASC")
        );

        // When: converting to Sort
        final Sort sort = metaData.toSort(columns);

        // Then: 'role' survives with DESC, nonSortableField is gone
        assertThat(sort.stream().count(), is(1L));
        final Sort.Order roleOrder = sort.getOrderFor("role");
        assertThat(roleOrder, is(notNullValue()));
        assertThat(roleOrder.getDirection(), is(equalTo(Sort.Direction.DESC)));
    }

    @Test
    @DisplayName("Should preserve original order of surviving valid columns")
    void shouldPreserveOriginalOrderOfSurvivingValidColumns() {
        // Given: valid columns 'role' then 'email', with an invalid column in between
        final TestMetaData metaData = new TestMetaData();
        final List<SortableColumn> columns = List.of(
            new SortableColumn("role", "ASC"),
            new SortableColumn("unknown", "DESC"),
            new SortableColumn("email", "DESC")
        );

        // When: converting to Sort
        final Sort sort = metaData.toSort(columns);

        // Then: 'role' is the primary sort, 'email' is secondary — unknown is gone
        assertThat(sort.stream().count(), is(2L));
        final List<Sort.Order> orders = sort.stream().toList();
        assertThat(orders.get(0).getProperty(), is("role"));
        assertThat(orders.get(1).getProperty(), is("user.email"));
    }

    @Test
    @DisplayName("Should not log a warning when all requested columns are valid")
    void shouldNotLogWarningWhenAllColumnsAreValid() {
        // Given: all requested columns are registered and sortable
        final TestMetaData metaData = new TestMetaData();
        final List<SortableColumn> columns = List.of(
            new SortableColumn("email", "ASC"),
            new SortableColumn("role", "DESC")
        );

        // When: converting to Sort
        metaData.toSort(columns);

        // Then: no warnings logged
        final List<ILoggingEvent> warnings = logAppender.list.stream()
            .filter(e -> e.getLevel() == Level.WARN)
            .toList();
        assertThat(warnings, hasSize(0));
    }

    @Test
    @DisplayName("Should treat unregistered and non-sortable columns identically — both stripped")
    void shouldTreatUnregisteredAndNonSortableColumnsIdentically() {
        // Given: one unregistered column and one registered-but-non-sortable column, no valid columns
        final TestMetaData metaData = new TestMetaData();
        final List<SortableColumn> columns = List.of(
            new SortableColumn("unknown", "ASC"),
            new SortableColumn("nonSortableField", "DESC")
        );

        // When: converting to Sort — must not throw
        final Sort sort = metaData.toSort(columns);

        // Then: result is unsorted — both discarded
        assertThat(sort.isUnsorted(), is(true));
    }

    @Test
    @DisplayName("Should handle duplicate valid column names without throwing")
    void shouldHandleDuplicateValidColumnNamesWithoutThrowing() {
        // Given: the same sortable column appears twice in the request
        final TestMetaData metaData = new TestMetaData();
        final List<SortableColumn> columns = List.of(
            new SortableColumn("email", "ASC"),
            new SortableColumn("email", "DESC")
        );

        // When: converting to Sort — must not throw
        final Sort sort = metaData.toSort(columns);

        // Then: result is non-null (duplicate handling delegated to Spring Sort)
        assertThat(sort, is(notNullValue()));
    }

    @Test
    @DisplayName("Should respect case-sensitivity of column names — exact-match only")
    void shouldRespectCaseSensitivityOfColumnNames() {
        // Given: 'Email' with a capital E does not match the registered 'email'
        final TestMetaData metaData = new TestMetaData();
        final List<SortableColumn> columns = List.of(new SortableColumn("Email", "ASC"));

        // When: converting to Sort
        final Sort sort = metaData.toSort(columns);

        // Then: 'Email' is treated as unknown, result is unsorted
        assertThat(sort.isUnsorted(), is(true));
    }

    // =========================================================================
    // Test fixture
    // =========================================================================

    static class TestMetaData extends AbstractSortSearchMetaData {

        public TestMetaData() {
            super();
            addField("email", "user.email", SearchType.FUZZY_TEXT, true);
            addField("role", "role", SearchType.ENUM, true);
            addField("nonSortableField", "col", SearchType.TEXT, false);
        }
    }
}
