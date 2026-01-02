package io.github.jframe.datasource.search.model;

import io.github.jframe.datasource.search.SearchOperator;
import io.github.jframe.datasource.search.fields.*;
import io.github.support.UnitTest;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.List;
import jakarta.persistence.criteria.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@DisplayName("SearchSpecification - Search Fields")
class JpaSearchSpecificationTest extends UnitTest {

    @Mock
    private Root<Object> root;
    @Mock
    private CriteriaQuery<?> query;
    @Mock
    private CriteriaBuilder cb;
    @Mock
    private Path<Object> path;
    @Mock
    private Expression<String> lowerPath;
    @Mock
    private Predicate predicate;
    @Mock
    private Predicate notPredicate;

    @BeforeEach
    public void setUp() {
        lenient().when(root.get(anyString())).thenReturn(path);
        lenient().when(path.in(anyCollection())).thenReturn(predicate);
        lenient().when(cb.lower(any())).thenReturn(lowerPath);
        lenient().when(cb.equal(any(), any())).thenReturn(predicate);
        lenient().when(cb.like(any(), anyString())).thenReturn(predicate);
        lenient().when(cb.greaterThanOrEqualTo(any(), any(LocalDateTime.class))).thenReturn(predicate);
        lenient().when(cb.lessThanOrEqualTo(any(), any(LocalDateTime.class))).thenReturn(predicate);

        lenient().when(cb.and(any(Predicate[].class))).thenReturn(predicate);
        lenient().when(cb.or(any(Predicate[].class))).thenReturn(predicate);
        lenient().when(cb.not(any(Predicate.class))).thenReturn(notPredicate);
    }

    @Test
    @DisplayName("Should create correct predicate for BooleanField")
    void testToPredicate_WithBooleanField() {
        final BooleanField field = new BooleanField("active", "true");
        final JpaSearchSpecification<Object> spec = new JpaSearchSpecification<>(Collections.singletonList(field));

        spec.toPredicate(root, query, cb);

        verify(root).get("active");
        verify(cb).equal(path, true);
    }

    @Test
    @DisplayName("Should create correct predicate for NumericField")
    void testToPredicate_WithNumericField() {
        final NumericField field = new NumericField("age", "25");
        final JpaSearchSpecification<Object> spec = new JpaSearchSpecification<>(Collections.singletonList(field));

        spec.toPredicate(root, query, cb);

        verify(root).get("age");
        verify(cb).equal(path, 25);
    }

    @Test
    @DisplayName("Should create correct inverse predicate for NumericField")
    void testToPredicate_WithInverseNumericField() {
        final NumericField field = new NumericField("age", "!25");
        final JpaSearchSpecification<Object> spec = new JpaSearchSpecification<>(Collections.singletonList(field));

        spec.toPredicate(root, query, cb);

        verify(root).get("age");
        verify(cb).equal(path, 25);
        verify(cb).not(any());
    }

    @Test
    @DisplayName("Should create correct predicate for DateField (From Date)")
    void testToPredicate_WithDateField_From() {
        final LocalDateTime now = LocalDateTime.now();
        final String nowStr = now.format(DateTimeFormatter.ISO_DATE_TIME);

        final DateField field = new DateField("createdAt", nowStr, null);
        final JpaSearchSpecification<Object> spec = new JpaSearchSpecification<>(Collections.singletonList(field));

        spec.toPredicate(root, query, cb);

        verify(root, times(2)).get("createdAt");

        verify(cb).greaterThanOrEqualTo(any(), eq(LocalDateTime.parse(nowStr, DateTimeFormatter.ISO_DATE_TIME)));
    }

    @Test
    @DisplayName("Should create correct predicate for DateField (To Date)")
    void testToPredicate_WithDateField_To() {
        final LocalDateTime now = LocalDateTime.now();
        final String nowStr = now.format(DateTimeFormatter.ISO_DATE_TIME);

        final DateField field = new DateField("createdAt", null, nowStr);
        final JpaSearchSpecification<Object> spec = new JpaSearchSpecification<>(Collections.singletonList(field));

        spec.toPredicate(root, query, cb);

        verify(root, times(2)).get("createdAt");

        verify(cb).lessThanOrEqualTo(any(), eq(LocalDateTime.parse(nowStr, DateTimeFormatter.ISO_DATE_TIME)));
    }

    @Test
    @DisplayName("Should create correct predicate for EnumField")
    void testToPredicate_WithEnumField() {
        final EnumField field = new EnumField("status", TestStatus.class, "ACTIVE");
        final JpaSearchSpecification<Object> spec = new JpaSearchSpecification<>(Collections.singletonList(field));

        spec.toPredicate(root, query, cb);

        verify(root).get("status");
        verify(cb).equal(path, TestStatus.ACTIVE);
    }

    @Test
    @DisplayName("Should create correct inverse predicate for EnumField")
    void testToPredicate_WithInverseEnumField() {
        final EnumField field = new EnumField("status", TestStatus.class, "!ACTIVE");
        final JpaSearchSpecification<Object> spec = new JpaSearchSpecification<>(Collections.singletonList(field));

        spec.toPredicate(root, query, cb);

        verify(root).get("status");
        verify(cb).equal(path, TestStatus.ACTIVE);
        verify(cb).not(any());
    }

    @Test
    @DisplayName("Should create correct predicate for MultiEnumField")
    void testToPredicate_WithMultiEnumField() {
        final List<String> enums = List.of("ACTIVE", "PENDING");
        final MultiEnumField field = new MultiEnumField("status", TestStatus.class, enums);
        final JpaSearchSpecification<Object> spec = new JpaSearchSpecification<>(Collections.singletonList(field));

        spec.toPredicate(root, query, cb);

        verify(root).get("status");
        verify(path).in(anyCollection());
    }

    @Test
    @DisplayName("Should create correct predicate for TextField")
    void testToPredicate_WithTextField() {
        final TextField field = new TextField("description", "test");
        final JpaSearchSpecification<Object> spec = new JpaSearchSpecification<>(Collections.singletonList(field));

        spec.toPredicate(root, query, cb);

        verify(root).get("description");
        verify(cb).equal(path, "test");
    }

    @Test
    @DisplayName("Should create correct inverse predicate for TextField")
    void testToPredicate_WithInverseTextField() {
        final TextField field = new TextField("description", "!test");
        final JpaSearchSpecification<Object> spec = new JpaSearchSpecification<>(Collections.singletonList(field));

        spec.toPredicate(root, query, cb);

        verify(root).get("description");
        verify(cb).equal(path, "test");
        verify(cb).not(any());
    }

    @Test
    @DisplayName("Should create correct predicate for MultiTextField")
    void testToPredicate_WithMultiTextField() {
        final List<String> values = List.of("A", "B");
        final MultiTextField field = new MultiTextField("category", values);
        final JpaSearchSpecification<Object> spec = new JpaSearchSpecification<>(Collections.singletonList(field));

        spec.toPredicate(root, query, cb);

        verify(root).get("category");
        verify(path).in(values);
    }

    @Test
    @DisplayName("Should create correct predicate for FuzzyTextField")
    void testToPredicate_WithFuzzyTextField() {
        final FuzzyTextField field = new FuzzyTextField("username", "admin");
        final JpaSearchSpecification<Object> spec = new JpaSearchSpecification<>(Collections.singletonList(field));

        spec.toPredicate(root, query, cb);

        verify(root).get("username");
        verify(cb).like(any(), eq("%admin%"));
    }

    @Test
    @DisplayName("Should create correct predicate for MultiFuzzyField with AND operator")
    void testToPredicate_WithMultiFuzzyField_AND() {
        final MultiFuzzyField field = new MultiFuzzyField("tags", SearchOperator.AND, "tag1 tag2");
        final JpaSearchSpecification<Object> spec = new JpaSearchSpecification<>(Collections.singletonList(field));

        spec.toPredicate(root, query, cb);

        verify(root, times(1)).get("tags");
        verify(cb, times(2)).like(any(), anyString());

        verify(cb, times(2)).and(any(Predicate[].class));
    }

    @Test
    @DisplayName("Should create correct predicate for MultiFuzzyField with OR operator")
    void testToPredicate_WithMultiFuzzyField_OR() {
        final MultiFuzzyField field = new MultiFuzzyField("tags", SearchOperator.OR, "tag1 tag2");
        final JpaSearchSpecification<Object> spec = new JpaSearchSpecification<>(Collections.singletonList(field));

        spec.toPredicate(root, query, cb);

        verify(root, times(1)).get("tags");
        verify(cb, times(2)).like(any(), anyString());

        verify(cb).or(any(Predicate[].class));
    }

    @Test
    @DisplayName("Should create correct predicate for MultiColumnFuzzyField")
    void testToPredicate_WithMultiColumnFuzzyField() {
        final List<String> columns = List.of("firstName", "lastName");
        final String searchValue = "john doe";
        final MultiColumnFuzzyField field = new MultiColumnFuzzyField(columns, searchValue);

        final JpaSearchSpecification<Object> spec = new JpaSearchSpecification<>(Collections.singletonList(field));

        final Predicate result = spec.toPredicate(root, query, cb);

        assertNotNull(result);

        verify(root, times(2)).get("firstName");
        verify(root, times(2)).get("lastName");

        verify(cb, times(4)).like(any(), anyString());
        verify(cb, times(2)).or(any(Predicate[].class));
        verify(cb, times(2)).and(any(Predicate[].class));
    }

    private enum TestStatus {
        ACTIVE,
        PENDING,
        INACTIVE
    }
}
