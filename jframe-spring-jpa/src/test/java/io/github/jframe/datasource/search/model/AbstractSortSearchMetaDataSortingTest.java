package io.github.jframe.datasource.search.model;

import io.github.jframe.datasource.search.SearchType;
import io.github.jframe.datasource.search.model.input.SortableColumn;
import io.github.support.UnitTest;

import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Sort;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("AbstractSortSearchMetaData Sorting Tests")
class AbstractSortSearchMetaDataSortingTest extends UnitTest {

    @Test
    @DisplayName("Should create Sort for single mapped field")
    void testSingleMappedField() {
        TestMetaData metaData = new TestMetaData();
        List<SortableColumn> columns = List.of(new SortableColumn("email", "ASC"));

        Sort sort = metaData.toSort(columns);

        assertNotNull(sort);
        Sort.Order order = sort.getOrderFor("user.email");
        assertNotNull(order);
        assertEquals(Sort.Direction.ASC, order.getDirection());
    }

    @Test
    @DisplayName("Should create Sort for multiple fields")
    void testMultipleFields() {
        TestMetaData metaData = new TestMetaData();
        List<SortableColumn> columns = List.of(
            new SortableColumn("email", "ASC"),
            new SortableColumn("role", "DESC")
        );

        Sort sort = metaData.toSort(columns);

        assertNotNull(sort);
        assertEquals(2, sort.stream().count());

        Sort.Order emailOrder = sort.getOrderFor("user.email");
        assertNotNull(emailOrder);
        assertEquals(Sort.Direction.ASC, emailOrder.getDirection());

        Sort.Order roleOrder = sort.getOrderFor("role");
        assertNotNull(roleOrder);
        assertEquals(Sort.Direction.DESC, roleOrder.getDirection());
    }

    @Test
    @DisplayName("Should throw exception when attempting to sort on unknown fields")
    void testThrowOnUnknownFields() {
        TestMetaData metaData = new TestMetaData();
        // "unknown" is not in metadata
        List<SortableColumn> columns = List.of(new SortableColumn("unknown", "ASC"));

        assertThrows(IllegalArgumentException.class, () -> metaData.toSort(columns));
    }

    @Test
    @DisplayName("Should handle empty sort list")
    void testEmptySortList() {
        TestMetaData metaData = new TestMetaData();
        Sort sort = metaData.toSort(Collections.emptyList());
        assertTrue(sort.isUnsorted());
    }

    @Test
    @DisplayName("Should throw exception when attempting to sort on non-sortable field if requested")
    void testThrowOnNonSortable() {
        TestMetaData metaData = new TestMetaData();
        List<SortableColumn> columns = List.of(new SortableColumn("nonSortableField", "ASC"));

        // The current implementation filters out known fields but checks size match at end
        // if orders.size() != sortOrders.size() throw exception
        assertThrows(IllegalArgumentException.class, () -> metaData.toSort(columns));
    }

    static class TestMetaData extends AbstractSortSearchMetaData {

        public TestMetaData() {
            super();
            addField("email", "user.email", SearchType.FUZZY_TEXT, true);
            addField("role", "role", SearchType.ENUM, true);
            addField("nonSortableField", "col", SearchType.TEXT, false);
        }
    }
}
