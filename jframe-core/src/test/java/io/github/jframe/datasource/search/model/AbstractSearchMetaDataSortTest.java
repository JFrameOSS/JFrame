package io.github.jframe.datasource.search.model;

import io.github.jframe.datasource.search.SearchType;
import io.github.jframe.datasource.search.model.input.SortableColumn;
import io.github.jframe.datasource.search.model.input.SortablePageInput;
import io.github.support.UnitTest;

import java.util.Collections;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;

@DisplayName("Unit Test - AbstractSearchMetaData sort resolution")
class AbstractSearchMetaDataSortTest extends UnitTest {

    @Test
    @DisplayName("Should derive default tiebreaker [id ASC] from tiebreakerProperty, empty when it is null")
    void shouldDeriveTiebreakerFromTiebreakerProperty() {
        // Given: Default metadata and metadata with the tiebreaker disabled
        final TestMetaData defaults = new TestMetaData();
        final TestMetaData disabled = new NoTiebreakerMetaData();

        // When: Reading the tiebreakers
        final List<SortableColumn> defaultTiebreaker = defaults.tiebreaker();
        final List<SortableColumn> disabledTiebreaker = disabled.tiebreaker();

        // Then: id ASC by default, none when disabled
        assertThat(defaultTiebreaker, contains(new SortableColumn("id", "ASC")));
        assertThat(disabledTiebreaker, is(empty()));
    }

    @Test
    @DisplayName("Should flag sort virtual when a non-leading column is virtual, keeping applied sort on the leading column")
    void shouldFlagVirtualWhenSecondaryColumnIsVirtual() {
        // Given: Mapped field first, virtual field second
        final SortablePageInput input = new SortablePageInput(
            0,
            25,
            List.of(new SortableColumn("name", "ASC"), new SortableColumn("virtual", "DESC")),
            Collections.emptyList()
        );

        // When: Resolving the core sort
        final ResolvedSortCore core = new TestMetaData().resolveCore(input);

        // Then: Virtual flag set, applied sort reflects the leading column
        assertThat(core.isVirtual(), is(true));
        assertThat(core.getAppliedSort().getField(), is(equalTo("name")));
    }

    static class TestMetaData extends AbstractSearchMetaData {

        TestMetaData() {
            addField("name", "name_col", SearchType.TEXT, true);
        }

        @Override
        protected Set<String> virtualSortFields() {
            return Set.of("virtual");
        }
    }


    static class NoTiebreakerMetaData extends TestMetaData {

        @Override
        protected String tiebreakerProperty() {
            return null;
        }
    }
}
