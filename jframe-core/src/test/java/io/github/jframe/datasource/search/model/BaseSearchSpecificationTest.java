package io.github.jframe.datasource.search.model;

import io.github.jframe.datasource.search.SearchOperator;
import io.github.jframe.datasource.search.fields.BooleanField;
import io.github.jframe.datasource.search.fields.DateField;
import io.github.jframe.datasource.search.fields.EnumField;
import io.github.jframe.datasource.search.fields.FuzzyTextField;
import io.github.jframe.datasource.search.fields.MultiColumnFuzzyField;
import io.github.jframe.datasource.search.fields.MultiEnumField;
import io.github.jframe.datasource.search.fields.MultiFuzzyField;
import io.github.jframe.datasource.search.fields.MultiNumericField;
import io.github.jframe.datasource.search.fields.MultiTextField;
import io.github.jframe.datasource.search.fields.NumericField;
import io.github.jframe.datasource.search.fields.NumericRangeField;
import io.github.jframe.datasource.search.fields.TextField;
import io.github.support.UnitTest;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
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
import static org.mockito.ArgumentMatchers.anyChar;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@DisplayName("SearchSpecification - Search Fields")
class BaseSearchSpecificationTest extends UnitTest {

    private static final char ESCAPE_CHAR = '\\';

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
        lenient().when(cb.like(any(), anyString(), anyChar())).thenReturn(predicate);
        lenient().when(cb.greaterThanOrEqualTo(any(), any(LocalDateTime.class))).thenReturn(predicate);
        lenient().when(cb.lessThanOrEqualTo(any(), any(LocalDateTime.class))).thenReturn(predicate);
        lenient().when(cb.greaterThanOrEqualTo(any(), any(Integer.class))).thenReturn(predicate);
        lenient().when(cb.lessThanOrEqualTo(any(), any(Integer.class))).thenReturn(predicate);

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
        verify(cb).like(any(), eq("%admin%"), eq(ESCAPE_CHAR));
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
        verify(cb, times(2)).like(any(), anyString(), eq(ESCAPE_CHAR));
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
        verify(cb, times(2)).like(any(), anyString(), eq(ESCAPE_CHAR));
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
        verify(cb, times(4)).like(any(), anyString(), eq(ESCAPE_CHAR));
        verify(cb, times(2)).or(any(Predicate[].class));
        verify(cb, times(2)).and(any(Predicate[].class));
    }

    @Test
    @DisplayName("Should create correct predicate for MultiNumericField")
    void testToPredicate_WithMultiNumericField() {
        // Given: A MultiNumericField for 'age' with multiple numeric values
        final List<String> values = List.of("18", "25", "30");
        final MultiNumericField field = new MultiNumericField("age", values);
        final BaseSearchSpecification<Object> spec = new BaseSearchSpecification<>(Collections.singletonList(field));

        // When: Building the predicate
        spec.toPredicate(root, query, cb);

        // Then: Root gets 'age' and path.in is called with the integer values list
        verify(root).get("age");
        verify(path).in(anyCollection());
    }

    @Test
    @DisplayName("Should create correct predicate for NumericRangeField with both from and to values")
    void testToPredicate_WithNumericRangeField_BothValues() {
        // Given: A NumericRangeField for 'age' with both from and to values
        final NumericRangeField field = new NumericRangeField("age", 18, 65);
        final BaseSearchSpecification<Object> spec = new BaseSearchSpecification<>(Collections.singletonList(field));

        // When: Building the predicate
        spec.toPredicate(root, query, cb);

        // Then: Root gets 'age' twice and both comparison predicates are created
        verify(root, times(2)).get("age");
        verify(cb).greaterThanOrEqualTo(any(), eq(18));
        verify(cb).lessThanOrEqualTo(any(), eq(65));
    }

    @Test
    @DisplayName("Should create only greaterThanOrEqualTo predicate for NumericRangeField with only fromValue")
    void testToPredicate_WithNumericRangeField_OnlyFromValue() {
        // Given: A NumericRangeField for 'age' with only fromValue set
        final NumericRangeField field = new NumericRangeField("age", 18, null);
        final BaseSearchSpecification<Object> spec = new BaseSearchSpecification<>(Collections.singletonList(field));

        // When: Building the predicate
        spec.toPredicate(root, query, cb);

        // Then: Only greaterThanOrEqualTo is called, not lessThanOrEqualTo
        verify(cb).greaterThanOrEqualTo(any(), eq(18));
        verify(cb, times(0)).lessThanOrEqualTo(any(), any(Integer.class));
    }

    @Test
    @DisplayName("Should create only lessThanOrEqualTo predicate for NumericRangeField with only toValue")
    void testToPredicate_WithNumericRangeField_OnlyToValue() {
        // Given: A NumericRangeField for 'age' with only toValue set
        final NumericRangeField field = new NumericRangeField("age", null, 65);
        final BaseSearchSpecification<Object> spec = new BaseSearchSpecification<>(Collections.singletonList(field));

        // When: Building the predicate
        spec.toPredicate(root, query, cb);

        // Then: Only lessThanOrEqualTo is called, not greaterThanOrEqualTo
        verify(cb, times(0)).greaterThanOrEqualTo(any(), any(Integer.class));
        verify(cb).lessThanOrEqualTo(any(), eq(65));
    }

    @Test
    @DisplayName("Should add no range predicates for NumericRangeField with both null values")
    void testToPredicate_WithNumericRangeField_BothNull() {
        // Given: A NumericRangeField for 'age' with both values null
        final NumericRangeField field = new NumericRangeField("age", null, null);
        final BaseSearchSpecification<Object> spec = new BaseSearchSpecification<>(Collections.singletonList(field));

        // When: Building the predicate
        spec.toPredicate(root, query, cb);

        // Then: Neither comparison predicate is created
        verify(cb, times(0)).greaterThanOrEqualTo(any(), any(Integer.class));
        verify(cb, times(0)).lessThanOrEqualTo(any(), any(Integer.class));
    }

    @Test
    @DisplayName("Should escape percent sign in FuzzyTextField term")
    void shouldEscapePercentSignInFuzzyTextFieldTerm() {
        // Given: A fuzzy term containing a literal percent sign
        final BaseSearchSpecification<Object> spec = aSpec(new FuzzyTextField("username", "50%off"));

        // When: Building the predicate
        spec.toPredicate(root, query, cb);

        // Then: The percent sign is escaped and the escape char is supplied
        verify(cb).like(any(), eq("%50\\%off%"), eq(ESCAPE_CHAR));
    }

    @Test
    @DisplayName("Should escape underscore in FuzzyTextField term")
    void shouldEscapeUnderscoreInFuzzyTextFieldTerm() {
        // Given: A fuzzy term containing a literal underscore
        final BaseSearchSpecification<Object> spec = aSpec(new FuzzyTextField("username", "john_doe"));

        // When: Building the predicate
        spec.toPredicate(root, query, cb);

        // Then: The underscore is escaped
        verify(cb).like(any(), eq("%john\\_doe%"), eq(ESCAPE_CHAR));
    }

    @Test
    @DisplayName("Should escape backslash in FuzzyTextField term")
    void shouldEscapeBackslashInFuzzyTextFieldTerm() {
        // Given: A fuzzy term containing the escape character itself
        final BaseSearchSpecification<Object> spec = aSpec(new FuzzyTextField("path", "c:\\temp"));

        // When: Building the predicate
        spec.toPredicate(root, query, cb);

        // Then: The backslash is doubled so it matches literally
        verify(cb).like(any(), eq("%c:\\\\temp%"), eq(ESCAPE_CHAR));
    }

    @Test
    @DisplayName("Should escape term consisting only of a percent sign")
    void shouldEscapeTermConsistingOnlyOfPercentSign() {
        // Given: A fuzzy term that is just a wildcard character
        final BaseSearchSpecification<Object> spec = aSpec(new FuzzyTextField("username", "%"));

        // When: Building the predicate
        spec.toPredicate(root, query, cb);

        // Then: It does not match everything — the percent is literal
        verify(cb).like(any(), eq("%\\%%"), eq(ESCAPE_CHAR));
    }

    @Test
    @DisplayName("Should escape all special characters in a single term in order")
    void shouldEscapeAllSpecialCharactersInSingleTerm() {
        // Given: A term mixing backslash, percent and underscore
        final BaseSearchSpecification<Object> spec = aSpec(new FuzzyTextField("username", "\\%_"));

        // When: Building the predicate
        spec.toPredicate(root, query, cb);

        // Then: Each character is escaped exactly once
        verify(cb).like(any(), eq("%\\\\\\%\\_%"), eq(ESCAPE_CHAR));
    }

    @Test
    @DisplayName("Should escape wildcards in each MultiFuzzyField term")
    void shouldEscapeWildcardsInEachMultiFuzzyFieldTerm() {
        // Given: A multi fuzzy field with wildcard characters in both terms
        final BaseSearchSpecification<Object> spec = aSpec(new MultiFuzzyField("tags", SearchOperator.AND, "a% b_"));

        // When: Building the predicate
        spec.toPredicate(root, query, cb);

        // Then: Each term is escaped individually
        verify(cb).like(any(), eq("%a\\%%"), eq(ESCAPE_CHAR));
        verify(cb).like(any(), eq("%b\\_%"), eq(ESCAPE_CHAR));
    }

    @Test
    @DisplayName("Should escape wildcards in MultiColumnFuzzyField term for every column")
    void shouldEscapeWildcardsInMultiColumnFuzzyFieldTermForEveryColumn() {
        // Given: A multi column fuzzy field with an underscore in the term
        final BaseSearchSpecification<Object> spec =
            aSpec(new MultiColumnFuzzyField(List.of("firstName", "lastName"), "jo_n"));

        // When: Building the predicate
        spec.toPredicate(root, query, cb);

        // Then: The escaped pattern is used for both columns
        verify(cb, times(2)).like(any(), eq("%jo\\_n%"), eq(ESCAPE_CHAR));
    }

    @Test
    @DisplayName("Should never use the two-argument like without escape char for fuzzy fields")
    void shouldNeverUseLikeWithoutEscapeCharForFuzzyFields() {
        // Given: One of each fuzzy field type
        final BaseSearchSpecification<Object> spec = new BaseSearchSpecification<>(
            List.of(
                new FuzzyTextField("username", "a"),
                new MultiFuzzyField("tags", SearchOperator.OR, "b"),
                new MultiColumnFuzzyField(List.of("firstName"), "c")
            )
        );

        // When: Building the predicate
        spec.toPredicate(root, query, cb);

        // Then: Only the escape-aware overload is used
        verify(cb, never()).like(any(), anyString());
        verify(cb, times(3)).like(any(), anyString(), eq(ESCAPE_CHAR));
    }

    @Test
    @DisplayName("Should lowercase fuzzy terms locale-independently under Turkish default locale")
    void shouldLowercaseFuzzyTermsLocaleIndependentlyUnderTurkishLocale() {
        // Given: Turkish default locale, where "I".toLowerCase() yields dotless "ı"
        final Locale original = Locale.getDefault();
        Locale.setDefault(Locale.forLanguageTag("tr"));
        try {
            final BaseSearchSpecification<Object> spec = new BaseSearchSpecification<>(
                List.of(
                    new FuzzyTextField("username", "ADMIN"),
                    new MultiFuzzyField("tags", SearchOperator.AND, "INFO"),
                    new MultiColumnFuzzyField(List.of("firstName"), "IVY")
                )
            );

            // When: Building the predicate
            spec.toPredicate(root, query, cb);

            // Then: "I" is lowercased to ASCII "i" in every fuzzy type
            verify(cb).like(any(), eq("%admin%"), eq(ESCAPE_CHAR));
            verify(cb).like(any(), eq("%info%"), eq(ESCAPE_CHAR));
            verify(cb).like(any(), eq("%ivy%"), eq(ESCAPE_CHAR));
        } finally {
            Locale.setDefault(original);
        }
    }

    @Test
    @DisplayName("Should not build like predicate when MultiFuzzyField value is null")
    void shouldNotBuildLikePredicateWhenMultiFuzzyFieldValueIsNull() {
        // Given: A multi fuzzy field with a null value
        final BaseSearchSpecification<Object> spec = aSpec(new MultiFuzzyField("tags", SearchOperator.AND, null));

        // When: Building the predicate
        spec.toPredicate(root, query, cb);

        // Then: No like predicate is created
        verify(cb, never()).like(any(), anyString(), anyChar());
    }

    @Test
    @DisplayName("Should not build like predicate when MultiColumnFuzzyField value is null")
    void shouldNotBuildLikePredicateWhenMultiColumnFuzzyFieldValueIsNull() {
        // Given: A multi column fuzzy field with a null value
        final BaseSearchSpecification<Object> spec = aSpec(new MultiColumnFuzzyField(List.of("firstName"), null));

        // When: Building the predicate
        spec.toPredicate(root, query, cb);

        // Then: No like predicate is created
        verify(cb, never()).like(any(), anyString(), anyChar());
    }

    private static BaseSearchSpecification<Object> aSpec(final SearchCriterium criterium) {
        return new BaseSearchSpecification<>(Collections.singletonList(criterium));
    }

    private enum TestStatus {
        ACTIVE,
        PENDING,
        INACTIVE
    }
}
