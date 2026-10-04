package io.github.jframe.datasource.search.model;

import io.github.jframe.datasource.search.JpaSearchSpecification;
import io.github.jframe.datasource.search.SearchType;
import io.github.jframe.datasource.search.model.input.SortableColumn;
import io.github.jframe.datasource.search.model.input.SortablePageInput;
import io.github.support.UnitTest;

import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Expression;
import jakarta.persistence.criteria.Nulls;
import jakarta.persistence.criteria.Order;
import jakarta.persistence.criteria.Path;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.springframework.data.jpa.domain.Specification;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.sameInstance;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@DisplayName("Unit Test - AbstractSortSearchMetaData virtual sort ordering")
class AbstractSortSearchMetaDataVirtualOrderingTest extends UnitTest {

    private static final long TENANT_ID = 42L;

    @Mock
    private Root<Object> root;
    @Mock
    private CriteriaQuery<Object> query;
    @Mock
    private CriteriaBuilder cb;
    @Mock
    private Predicate conjunction;

    @Mock
    private Path<String> namePath;
    @Mock
    private Path<String> firstNamePath;
    @Mock
    private Path<String> lastNamePath;
    @Mock
    private Path<Long> idPath;
    @Mock
    private Path<Long> idValuePath;
    @Mock
    private Path<Instant> createdAtPath;
    @Mock
    private Path<Object> tenantPath;
    @Mock
    private Path<Long> tenantIdPath;

    @Mock
    private Expression<Long> scoreExpr;
    @Mock
    private Expression<String> ownerLastExpr;
    @Mock
    private Expression<String> ownerFirstExpr;

    @Mock
    private Expression<String> lowerName;
    @Mock
    private Expression<String> lowerFirstName;
    @Mock
    private Expression<String> lowerLastName;
    @Mock
    private Expression<String> lowerOwnerLast;
    @Mock
    private Expression<String> lowerOwnerFirst;

    @Captor
    private ArgumentCaptor<List<Order>> ordersCaptor;

    @BeforeEach
    @Override
    public void setUp() {
        lenient().doReturn(Object.class).when(query).getResultType();
        lenient().when(cb.conjunction()).thenReturn(conjunction);
        lenient().when(cb.asc(any(), any(Nulls.class)))
            .thenAnswer(inv -> new TestOrder(inv.getArgument(0), true, inv.getArgument(1)));
        lenient().when(cb.desc(any(), any(Nulls.class)))
            .thenAnswer(inv -> new TestOrder(inv.getArgument(0), false, inv.getArgument(1)));

        lenient().doReturn(namePath).when(root).get("name_col");
        lenient().doReturn(firstNamePath).when(root).get("first_name");
        lenient().doReturn(lastNamePath).when(root).get("last_name");
        lenient().doReturn(idPath).when(root).get("id");
        lenient().doReturn(idValuePath).when(idPath).get("value");
        lenient().doReturn(createdAtPath).when(root).get("createdAt");
        lenient().doReturn(tenantPath).when(root).get("tenant");
        lenient().doReturn(tenantIdPath).when(tenantPath).get("id");

        lenient().doReturn(String.class).when(namePath).getJavaType();
        lenient().doReturn(String.class).when(firstNamePath).getJavaType();
        lenient().doReturn(String.class).when(lastNamePath).getJavaType();
        lenient().doReturn(String.class).when(ownerLastExpr).getJavaType();
        lenient().doReturn(String.class).when(ownerFirstExpr).getJavaType();
        lenient().doReturn(Long.class).when(idPath).getJavaType();
        lenient().doReturn(Long.class).when(idValuePath).getJavaType();
        lenient().doReturn(Long.class).when(scoreExpr).getJavaType();
        lenient().doReturn(Instant.class).when(createdAtPath).getJavaType();

        lenient().when(cb.lower(namePath)).thenReturn(lowerName);
        lenient().when(cb.lower(firstNamePath)).thenReturn(lowerFirstName);
        lenient().when(cb.lower(lastNamePath)).thenReturn(lowerLastName);
        lenient().when(cb.lower(ownerLastExpr)).thenReturn(lowerOwnerLast);
        lenient().when(cb.lower(ownerFirstExpr)).thenReturn(lowerOwnerFirst);
    }

    @Test
    @DisplayName("Should order by registered expression then tiebreaker, without lower(), when virtual field leads")
    void shouldOrderByExpressionThenTiebreakerWhenVirtualFieldLeads() {
        // Given: Metadata with a Long-typed sort expression, sorted by it DESC
        final VirtualMetaData meta = aVirtualMetaData();
        final SortablePageInput input = aSortInput(new SortableColumn("score", "DESC"));

        // When: Building the specification predicate
        final JpaSearchSpecification<Object> spec = meta.toSearchSpecification(input);
        final Predicate predicate = spec.toPredicate(root, query, cb);

        // Then: Expression DESC nulls last, then id ASC nulls last; no lower(); key allowed
        verify(query).orderBy(ordersCaptor.capture());
        assertThat(ordersCaptor.getValue(), contains(aDescendingOrder(scoreExpr), anAscendingOrder(idPath)));
        verify(cb, never()).lower(any());
        assertThat(predicate, is(sameInstance(conjunction)));
        assertThat(meta.getAllowedSortFields(), hasItem("score"));
    }

    @Test
    @DisplayName("Should keep request order and lower() only String columns when virtual field is secondary")
    void shouldKeepRequestOrderAndLowerOnlyStringsWhenVirtualFieldIsSecondary() {
        // Given: Mapped String, virtual Long, virtual multi-expression String, mapped multi-column
        final SortablePageInput input = aSortInput(
            new SortableColumn("name", "ASC"),
            new SortableColumn("score", "DESC"),
            new SortableColumn("owner", "ASC"),
            new SortableColumn("fullName", "DESC")
        );

        final VirtualMetaData meta = aVirtualMetaData();

        // When: Resolving the sort and building the specification predicate
        final ResolvedSort resolved = meta.resolveSort(input);
        meta.<Object>toSearchSpecification(input).toPredicate(root, query, cb);

        // Then: Sort flagged virtual with unsorted Pageable
        assertThat(resolved.isVirtual(), is(true));
        assertThat(resolved.getPageable().getSort().isUnsorted(), is(true));

        // And: Every column/expression ordered in request order with its own direction; only Strings lower-cased
        verify(query).orderBy(ordersCaptor.capture());
        assertThat(
            ordersCaptor.getValue(),
            contains(
                anAscendingOrder(lowerName),
                aDescendingOrder(scoreExpr),
                anAscendingOrder(lowerOwnerLast),
                anAscendingOrder(lowerOwnerFirst),
                aDescendingOrder(lowerFirstName),
                aDescendingOrder(lowerLastName),
                anAscendingOrder(idPath)
            )
        );
    }

    @Test
    @DisplayName("Should apply expression ordering when default sort uses a virtual key and request has no sort")
    void shouldApplyOrderingWhenDefaultSortIsVirtual() {
        // Given: Metadata defaulting to score DESC, request without sort
        final VirtualMetaData meta = new VirtualDefaultSortMetaData(scoreExpr, ownerLastExpr, ownerFirstExpr);
        final SortablePageInput input = new SortablePageInput(0, 25, Collections.emptyList(), Collections.emptyList());

        // When: Building the specification predicate
        meta.<Object>toSearchSpecification(input).toPredicate(root, query, cb);

        // Then: Default virtual sort is applied in the query
        verify(query).orderBy(ordersCaptor.capture());
        assertThat(ordersCaptor.getValue(), contains(aDescendingOrder(scoreExpr), anAscendingOrder(idPath)));
    }

    @ParameterizedTest(name = "result type {0}")
    @ValueSource(
        classes = {
            Long.class,
            long.class
        }
    )
    @DisplayName("Should not order count queries but still return the search predicate")
    void shouldNotOrderWhenCountQuery(final Class<?> resultType) {
        // Given: A count query and a virtual sort
        doReturn(resultType).when(query).getResultType();
        final SortablePageInput input = aSortInput(new SortableColumn("score", "ASC"));

        // When: Building the specification predicate
        final Predicate predicate = aVirtualMetaData().<Object>toSearchSpecification(input).toPredicate(root, query, cb);

        // Then: No ordering, predicate intact
        verify(query, never()).orderBy(anyList());
        assertThat(predicate, is(sameInstance(conjunction)));
    }

    @Test
    @DisplayName("Should not fail nor order when query is null")
    void shouldNotOrderWhenQueryIsNull() {
        // Given: A virtual sort
        final SortablePageInput input = aSortInput(new SortableColumn("score", "ASC"));

        // When: Building the predicate without a query
        final Predicate predicate = aVirtualMetaData().<Object>toSearchSpecification(input).toPredicate(root, null, cb);

        // Then: Predicate is still returned and no ordering expression built
        assertThat(predicate, is(sameInstance(conjunction)));
        verify(cb, never()).asc(any(), any(Nulls.class));
        verify(cb, never()).desc(any(), any(Nulls.class));
    }

    @Test
    @DisplayName("Should not append tiebreaker twice when sort already contains it")
    void shouldNotDuplicateTiebreakerWhenAlreadyInSort() {
        // Given: Virtual sort followed by explicit id DESC
        final SortablePageInput input = aSortInput(new SortableColumn("score", "ASC"), new SortableColumn("id", "DESC"));

        // When: Building the specification predicate
        aVirtualMetaData().<Object>toSearchSpecification(input).toPredicate(root, query, cb);

        // Then: id appears once, in the requested direction
        verify(query).orderBy(ordersCaptor.capture());
        assertThat(ordersCaptor.getValue(), contains(anAscendingOrder(scoreExpr), aDescendingOrder(idPath)));
    }

    @Test
    @DisplayName("Should append composite tiebreaker in order with own directions and resolve dot paths")
    void shouldAppendCompositeTiebreakerWithDotPaths() {
        // Given: Tiebreaker override [createdAt DESC, id.value ASC]
        final VirtualMetaData meta = new CompositeTiebreakerMetaData(scoreExpr, ownerLastExpr, ownerFirstExpr);
        final SortablePageInput input = aSortInput(new SortableColumn("score", "ASC"));

        // When: Building the specification predicate
        meta.<Object>toSearchSpecification(input).toPredicate(root, query, cb);

        // Then: Tiebreaker columns follow the expression; non-String paths not lower-cased
        verify(query).orderBy(ordersCaptor.capture());
        assertThat(
            ordersCaptor.getValue(),
            contains(anAscendingOrder(scoreExpr), aDescendingOrder(createdAtPath), anAscendingOrder(idValuePath))
        );
        verify(cb, never()).lower(any());
    }

    @Test
    @DisplayName("Should skip virtual key without expression but still order mapped columns and tiebreaker")
    void shouldSkipVirtualKeyWithoutExpression() {
        // Given: Sort by a virtualSortFields()-only key, then a mapped field
        final SortablePageInput input = aSortInput(new SortableColumn("manual", "ASC"), new SortableColumn("name", "DESC"));

        // When: Building the specification predicate
        aVirtualMetaData().<Object>toSearchSpecification(input).toPredicate(root, query, cb);

        // Then: Only the mapped column and tiebreaker are ordered
        verify(query).orderBy(ordersCaptor.capture());
        assertThat(ordersCaptor.getValue(), contains(aDescendingOrder(lowerName), anAscendingOrder(idPath)));
    }

    @Test
    @DisplayName("Should leave ordering to the Pageable when sort is not virtual")
    void shouldNotOrderWhenSortIsNotVirtual() {
        // Given: Mapped-only sort
        final SortablePageInput input = aSortInput(new SortableColumn("name", "ASC"));

        // When: Building the specification predicate
        aVirtualMetaData().<Object>toSearchSpecification(input).toPredicate(root, query, cb);

        // Then: Specification does not touch ordering
        verify(query, never()).orderBy(anyList());
    }

    @Test
    @DisplayName("Should apply virtual ordering exactly once in scoped specification")
    void shouldApplyOrderingOnceInScopedSpecification() {
        // Given: Virtual sort with tenant scope
        final SortablePageInput input = aSortInput(new SortableColumn("score", "DESC"));

        // When: Building the scoped predicate
        final Specification<Object> spec = aVirtualMetaData().toSearchSpecification(input, "tenant.id", TENANT_ID);
        spec.toPredicate(root, query, cb);

        // Then: Scope applied and ordering set once
        verify(cb).equal(tenantIdPath, TENANT_ID);
        verify(query, times(1)).orderBy(ordersCaptor.capture());
        assertThat(ordersCaptor.getValue(), contains(aDescendingOrder(scoreExpr), anAscendingOrder(idPath)));
    }

    // -------------------------------------------------------------------------
    // Factories
    // -------------------------------------------------------------------------

    private VirtualMetaData aVirtualMetaData() {
        return new VirtualMetaData(scoreExpr, ownerLastExpr, ownerFirstExpr);
    }

    private static SortablePageInput aSortInput(final SortableColumn... columns) {
        return new SortablePageInput(0, 25, List.of(columns), Collections.emptyList());
    }

    private static Order anAscendingOrder(final Expression<?> expression) {
        return new TestOrder(expression, true, Nulls.LAST);
    }

    private static Order aDescendingOrder(final Expression<?> expression) {
        return new TestOrder(expression, false, Nulls.LAST);
    }

    // -------------------------------------------------------------------------
    // Fixtures
    // -------------------------------------------------------------------------

    /** Value-equal {@link Order} so captured orders can be compared. */
    private record TestOrder(Expression<?> expression, boolean ascending, Nulls nulls) implements Order {

        @Override
        public Order reverse() {
            return new TestOrder(expression, !ascending, nulls);
        }

        @Override
        public boolean isAscending() {
            return ascending;
        }

        @Override
        public Nulls getNullPrecedence() {
            return nulls;
        }

        @Override
        public Expression<?> getExpression() {
            return expression;
        }
    }


    static class VirtualMetaData extends AbstractSortSearchMetaData {

        VirtualMetaData(final Expression<?> score, final Expression<?> ownerLast, final Expression<?> ownerFirst) {
            addField("name", "name_col", SearchType.TEXT, true);
            addField("id", "id", SearchType.NUMERIC, true);
            addField("fullName", List.of("first_name", "last_name"), SearchType.MULTI_COLUMN_FUZZY, true);
            addSortExpression("score", (from, criteriaQuery, builder) -> List.of(score));
            addSortExpression("owner", (from, criteriaQuery, builder) -> List.of(ownerLast, ownerFirst));
        }

        @Override
        protected Set<String> virtualSortFields() {
            return Set.of("manual");
        }
    }


    static class VirtualDefaultSortMetaData extends VirtualMetaData {

        VirtualDefaultSortMetaData(final Expression<?> score, final Expression<?> ownerLast, final Expression<?> ownerFirst) {
            super(score, ownerLast, ownerFirst);
        }

        @Override
        protected List<SortableColumn> defaultSort() {
            return List.of(new SortableColumn("score", "DESC"));
        }
    }


    static class CompositeTiebreakerMetaData extends VirtualMetaData {

        CompositeTiebreakerMetaData(final Expression<?> score, final Expression<?> ownerLast, final Expression<?> ownerFirst) {
            super(score, ownerLast, ownerFirst);
        }

        @Override
        protected List<SortableColumn> tiebreaker() {
            return List.of(new SortableColumn("createdAt", "DESC"), new SortableColumn("id.value", "ASC"));
        }
    }
}
