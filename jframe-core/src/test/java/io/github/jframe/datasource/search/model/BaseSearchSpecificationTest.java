package io.github.jframe.datasource.search.model;

import io.github.jframe.datasource.search.SearchOperator;
import io.github.jframe.datasource.search.fields.BooleanField;
import io.github.jframe.datasource.search.fields.DateField;
import io.github.jframe.datasource.search.fields.EnumField;
import io.github.jframe.datasource.search.fields.FuzzyTextField;
import io.github.jframe.datasource.search.fields.MultiColumnFuzzyField;
import io.github.jframe.datasource.search.fields.MultiEnumField;
import io.github.jframe.datasource.search.fields.MultiFuzzyField;
import io.github.jframe.datasource.search.fields.MultiTextField;
import io.github.jframe.datasource.search.fields.NumericField;
import io.github.jframe.datasource.search.fields.TextField;
import io.github.support.UnitTest;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.List;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Expression;
import jakarta.persistence.criteria.Path;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@DisplayName("SearchSpecification - Search Fields")
class BaseSearchSpecificationTest extends UnitTest {

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
        // Given: A BooleanField for the 'active' field with value 'true'
        final BooleanField field = new BooleanField("active", "true");
        final BaseSearchSpecification<Object> spec = new BaseSearchSpecification<>(Collections.singletonList(field));

        // When: Building the predicate
        spec.toPredicate(root, query, cb);

        // Then: Root gets 'active' and cb.equal is called with boolean true
        verify(root).get("active");
        verify(cb).equal(path, true);
    }

    @Test
    @DisplayName("Should create correct predicate for NumericField")
    void testToPredicate_WithNumericField() {
        // Given: A NumericField for 'age' with value '25'
        final NumericField field = new NumericField("age", "25");
        final BaseSearchSpecification<Object> spec = new BaseSearchSpecification<>(Collections.singletonList(field));

        // When: Building the predicate
        spec.toPredicate(root, query, cb);

        // Then: Root gets 'age' and cb.equal is called with integer 25
        verify(root).get("age");
        verify(cb).equal(path, 25);
    }

    @Test
    @DisplayName("Should create correct inverse predicate for NumericField")
    void testToPredicate_WithInverseNumericField() {
        // Given: A NumericField for 'age' with inverse value '!25'
        final NumericField field = new NumericField("age", "!25");
        final BaseSearchSpecification<Object> spec = new BaseSearchSpecification<>(Collections.singletonList(field));

        // When: Building the predicate
        spec.toPredicate(root, query, cb);

        // Then: Root gets 'age', cb.equal and cb.not are called
        verify(root).get("age");
        verify(cb).equal(path, 25);
        verify(cb).not(any());
    }

    @Test
    @DisplayName("Should create correct predicate for DateField (From Date)")
    void testToPredicate_WithDateField_From() {
        // Given: A DateField with a from-date and no to-date
        final LocalDateTime now = LocalDateTime.now();
        final String nowStr = now.format(DateTimeFormatter.ISO_DATE_TIME);
        final DateField field = new DateField("createdAt", nowStr, null);
        final BaseSearchSpecification<Object> spec = new BaseSearchSpecification<>(Collections.singletonList(field));

        // When: Building the predicate
        spec.toPredicate(root, query, cb);

        // Then: Root gets 'createdAt' twice and greaterThanOrEqualTo is called
        verify(root, times(2)).get("createdAt");
        verify(cb).greaterThanOrEqualTo(any(), eq(LocalDateTime.parse(nowStr, DateTimeFormatter.ISO_DATE_TIME)));
    }

    @Test
    @DisplayName("Should create correct predicate for DateField (To Date)")
    void testToPredicate_WithDateField_To() {
        // Given: A DateField with a to-date and no from-date
        final LocalDateTime now = LocalDateTime.now();
        final String nowStr = now.format(DateTimeFormatter.ISO_DATE_TIME);
        final DateField field = new DateField("createdAt", null, nowStr);
        final BaseSearchSpecification<Object> spec = new BaseSearchSpecification<>(Collections.singletonList(field));

        // When: Building the predicate
        spec.toPredicate(root, query, cb);

        // Then: Root gets 'createdAt' twice and lessThanOrEqualTo is called
        verify(root, times(2)).get("createdAt");
        verify(cb).lessThanOrEqualTo(any(), eq(LocalDateTime.parse(nowStr, DateTimeFormatter.ISO_DATE_TIME)));
    }

    @Test
    @DisplayName("Should create correct predicate for EnumField")
    void testToPredicate_WithEnumField() {
        // Given: An EnumField for 'status' with value 'ACTIVE'
        final EnumField field = new EnumField("status", TestStatus.class, "ACTIVE");
        final BaseSearchSpecification<Object> spec = new BaseSearchSpecification<>(Collections.singletonList(field));

        // When: Building the predicate
        spec.toPredicate(root, query, cb);

        // Then: Root gets 'status' and cb.equal is called with enum value
        verify(root).get("status");
        verify(cb).equal(path, TestStatus.ACTIVE);
    }

    @Test
    @DisplayName("Should create correct inverse predicate for EnumField")
    void testToPredicate_WithInverseEnumField() {
        // Given: An EnumField for 'status' with inverse value '!ACTIVE'
        final EnumField field = new EnumField("status", TestStatus.class, "!ACTIVE");
        final BaseSearchSpecification<Object> spec = new BaseSearchSpecification<>(Collections.singletonList(field));

        // When: Building the predicate
        spec.toPredicate(root, query, cb);

        // Then: Root gets 'status', cb.equal and cb.not are called
        verify(root).get("status");
        verify(cb).equal(path, TestStatus.ACTIVE);
        verify(cb).not(any());
    }

    @Test
    @DisplayName("Should create correct predicate for MultiEnumField")
    void testToPredicate_WithMultiEnumField() {
        // Given: A MultiEnumField for 'status' with multiple enum values
        final List<String> enums = List.of("ACTIVE", "PENDING");
        final MultiEnumField field = new MultiEnumField("status", TestStatus.class, enums);
        final BaseSearchSpecification<Object> spec = new BaseSearchSpecification<>(Collections.singletonList(field));

        // When: Building the predicate
        spec.toPredicate(root, query, cb);

        // Then: Root gets 'status' and path.in is called with collection
        verify(root).get("status");
        verify(path).in(anyCollection());
    }

    @Test
    @DisplayName("Should create correct predicate for TextField")
    void testToPredicate_WithTextField() {
        // Given: A TextField for 'description' with exact value 'test'
        final TextField field = new TextField("description", "test");
        final BaseSearchSpecification<Object> spec = new BaseSearchSpecification<>(Collections.singletonList(field));

        // When: Building the predicate
        spec.toPredicate(root, query, cb);

        // Then: Root gets 'description' and cb.equal is called with the string value
        verify(root).get("description");
        verify(cb).equal(path, "test");
    }

    @Test
    @DisplayName("Should create correct inverse predicate for TextField")
    void testToPredicate_WithInverseTextField() {
        // Given: A TextField for 'description' with inverse value '!test'
        final TextField field = new TextField("description", "!test");
        final BaseSearchSpecification<Object> spec = new BaseSearchSpecification<>(Collections.singletonList(field));

        // When: Building the predicate
        spec.toPredicate(root, query, cb);

        // Then: Root gets 'description', cb.equal and cb.not are called
        verify(root).get("description");
        verify(cb).equal(path, "test");
        verify(cb).not(any());
    }

    @Test
    @DisplayName("Should create correct predicate for MultiTextField")
    void testToPredicate_WithMultiTextField() {
        // Given: A MultiTextField for 'category' with multiple text values
        final List<String> values = List.of("A", "B");
        final MultiTextField field = new MultiTextField("category", values);
        final BaseSearchSpecification<Object> spec = new BaseSearchSpecification<>(Collections.singletonList(field));

        // When: Building the predicate
        spec.toPredicate(root, query, cb);

        // Then: Root gets 'category' and path.in is called with the values list
        verify(root).get("category");
        verify(path).in(values);
    }

    @Test
    @DisplayName("Should create correct predicate for FuzzyTextField")
    void testToPredicate_WithFuzzyTextField() {
        // Given: A FuzzyTextField for 'username' with value 'admin'
        final FuzzyTextField field = new FuzzyTextField("username", "admin");
        final BaseSearchSpecification<Object> spec = new BaseSearchSpecification<>(Collections.singletonList(field));

        // When: Building the predicate
        spec.toPredicate(root, query, cb);

        // Then: Root gets 'username' and cb.like is called with wildcard pattern
        verify(root).get("username");
        verify(cb).like(any(), eq("%admin%"));
    }

    @Test
    @DisplayName("Should create correct predicate for MultiFuzzyField with AND operator")
    void testToPredicate_WithMultiFuzzyField_AND() {
        // Given: A MultiFuzzyField for 'tags' with AND operator and two terms
        final MultiFuzzyField field = new MultiFuzzyField("tags", SearchOperator.AND, "tag1 tag2");
        final BaseSearchSpecification<Object> spec = new BaseSearchSpecification<>(Collections.singletonList(field));

        // When: Building the predicate
        spec.toPredicate(root, query, cb);

        // Then: Root gets 'tags', cb.like is called twice, cb.and is called twice
        verify(root, times(1)).get("tags");
        verify(cb, times(2)).like(any(), anyString());
        verify(cb, times(2)).and(any(Predicate[].class));
    }

    @Test
    @DisplayName("Should create correct predicate for MultiFuzzyField with OR operator")
    void testToPredicate_WithMultiFuzzyField_OR() {
        // Given: A MultiFuzzyField for 'tags' with OR operator and two terms
        final MultiFuzzyField field = new MultiFuzzyField("tags", SearchOperator.OR, "tag1 tag2");
        final BaseSearchSpecification<Object> spec = new BaseSearchSpecification<>(Collections.singletonList(field));

        // When: Building the predicate
        spec.toPredicate(root, query, cb);

        // Then: Root gets 'tags', cb.like is called twice, cb.or is called
        verify(root, times(1)).get("tags");
        verify(cb, times(2)).like(any(), anyString());
        verify(cb).or(any(Predicate[].class));
    }

    @Test
    @DisplayName("Should create correct predicate for MultiColumnFuzzyField")
    void testToPredicate_WithMultiColumnFuzzyField() {
        // Given: A MultiColumnFuzzyField across firstName and lastName with two search terms
        final List<String> columns = List.of("firstName", "lastName");
        final String searchValue = "john doe";
        final MultiColumnFuzzyField field = new MultiColumnFuzzyField(columns, searchValue);
        final BaseSearchSpecification<Object> spec = new BaseSearchSpecification<>(Collections.singletonList(field));

        // When: Building the predicate
        final Predicate result = spec.toPredicate(root, query, cb);

        // Then: Result is not null and correct number of root.get, cb.like, cb.or, cb.and calls are made
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
